import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import scala.language.postfixOps

class ThrottlingServiceSimulation extends Simulation {
  val authTokenFeeder = Array(
    Map("token" -> "tk1"),
    Map("token" -> "tk2"),
    Map("token" -> "tk3")
  ).random

  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    //.doNotTrackHeader("1")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .acceptEncodingHeader("gzip, deflate")
    .userAgentHeader("Mozilla/5.0 (Windows NT 5.1; rv:31.0) Gecko/20100101 Firefox/31.0")

  val regScn = scenario("RegisteredUserSimulation")
    .feed(authTokenFeeder)
    .exec(http("request_1")
      .get("/")
      .header("Authentication", "${token}"))
    //.pause(1)

  val unauthScn = scenario("UnauthenticatedUserSimulation")
    .exec(http("request_noAuth")
      .get("/")
    )
  val noThrottleScn = scenario("NoThrottleSimulation")
    .exec(http("request_noThrottle")
      .get("/noThrottle"))

  setUp(
    unauthScn.inject(
      constantUsersPerSec(20) during (5 seconds)
    ).protocols(httpProtocol) /*andThen
      noThrottleScn.inject(
        constantUsersPerSec(20) during (5 seconds)
      ).protocols(httpProtocol)*/
  ).assertions(
    global.successfulRequests.percent.between(40,60) //around(50, 5)
  )
}
