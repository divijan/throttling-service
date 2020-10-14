This is a test assignment I received in 2018 the first time. In 2020 I was asked to complete it again. This time I actually got round to it

Goal
----
To provide Service Level Agreement (SLA) for our REST endpoint the
throttling service should be implemented in order to limit the requests per
second (RPS) for each user.
Assume you already have a service, which returns maximum allowed
RPS and username of the user by the token from the request
`Authorization` header:

```
case class Sla (user:String, rps:Int)

trait SlaService {
  def getSlaByToken(token: String): Future[Sla]
}
```

Service to implement
---------
ThrottlingService might look like:
```
trait ThrottlingService {
  val graceRps:Int // configurable
  val slaService: SlaService // use mocks/stubs for testing
  // Should return true if the request is within allowed RPS.
  def isRequestAllowed(token:Option[String]): Boolean
}
```

Rules for RPS counting
-------
1. If no token provided, assume the client is unauthorized.
2. All unauthorized user's requests are limited by GraceRps
3. If request has a token, but slaService has not returned any info yet,
treat it as unauthorized user
4. RPS should be counted per user, as the same user might use
different tokens for authorization
5. SLA should be counted by intervals of 1/10 second (i.e. if RPS
limit is reached, after 1/10 second ThrottlingService should allow
10% more requests)
6. SLA information changes quite rarely and SlaService is quite
costly to call (~250ms per request), so consider caching SLA
requests. Also, you should not query the service, if the same token
request is already in progress.
7. Consider that REST service average response time is below 5ms,
ThrottlingService shouldn’t impact REST service SLA.

Acceptance Criteria
-------
1. Implement `ThrottlingService`.
2. Cover the code with the tests that prove the validity of the code.
3. Implement the load test that proves that for *N* users, *K* RPS during *T*
seconds around *T\*N\*K* requests were successful. 
4. Measure the
overhead of using `ThrottlingService`, compared with same
rest endpoint without `ThrottlingService`.

Our actual REST services use scala/java, spray, akka, maven.
For this assignment you can use any frameworks you prefer.

© 2020 Yaroslav Ilich