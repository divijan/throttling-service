import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.yar.sla.MockThrottlingService
import akka.http.scaladsl.model.headers._

import scala.io.StdIn

object WebServer {
  def extractAuthToken: HttpHeader => Option[String] = {
    case r: RawHeader if r.name == "Authentication" => Some(r.value)
    case x         => None
  }

  def main(args: Array[String]) {
    implicit val system = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    val users = Map(
      "tk1" -> "John",
      "tk3" -> "John",
      "tk2" -> "Chris"
    )

    def greetUser(authToken: Option[String], message: Option[String]) = complete {
      val userName = authToken.flatMap(users.get).fold("guest")(identity)
      s"Hello $userName!" + message.fold("")(" " + _)
    }

    val route = optionalHeaderValueByName("Authorization") { authToken =>
      concat (
        path("noThrottle") {
          get(greetUser(authToken, None))
        },
        pathSingleSlash {
          get {
            if (MockThrottlingService.isRequestAllowed(authToken)) {
              greetUser(authToken, Some("Welcome to ThrottlingService!"))
            } else {
              complete(StatusCodes.TooManyRequests)
            }
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