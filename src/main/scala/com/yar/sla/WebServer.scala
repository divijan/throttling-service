import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.yar.sla._
import akka.http.scaladsl.model.headers._
import com.typesafe.config.ConfigFactory

import scala.concurrent.Future
import scala.io.StdIn

object WebServer {
  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.dispatcher

  val slaService: SlaService = new SlaService {
    val table = Map(
      "tk2" -> Sla("John", 30),
      "tk1" -> Sla("Chris", 15),
      "tk3" -> Sla("John", 30)
    )

    override def getSlaByToken(token: String): Future[Sla] = {
      Future {
        Thread.sleep(250) //does something heavy
        table(token)
      }// no requirements what to do if no Sla found for token, so we just fail inside the Future
    }
  }
  val ts = new MyThrottlingService(ConfigFactory.load().getInt("graceRps"), slaService)

  val users = Map(
    "tk1" -> "John",
    "tk3" -> "John",
    "tk2" -> "Chris"
  )

  def main(args: Array[String]) {
    def greetUser(authToken: Option[String], message: Option[String]) = complete {
      val userName = authToken.flatMap(users.get).fold("guest")(identity)
      s"Hello $userName!" + message.fold("")(" " + _)
    }

    def extractAuthToken: HttpHeader => Option[String] = {
      case r: RawHeader if r.name == "Authentication" => Some(r.value)
      case x         => None
    }

    val route = optionalHeaderValueByName("Authorization") { authToken =>
      concat (
        path("noThrottle") & get & greetUser(authToken, None),
        (pathSingleSlash & get) {
          if (ts.isRequestAllowed(authToken)) {
            greetUser(authToken, Some("Welcome to ThrottlingService!"))
          } else {
            complete(StatusCodes.TooManyRequests)
          }
        }
      )
    }

    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}