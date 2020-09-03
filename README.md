1. If no token provided, assume the client is unauthorized.
2. All unauthorized user's requests are limited by GraceRps
3. If request has a token, but slaService has not returned any info yet,
treat it as unauthorized user
4. RPS should be counted per user, as the same user might use
different tokens for authorization
5. SLA should be counted by intervals of 1/10 second (i.e. if RPS
limit is reached, after 1/10 second ThrottlingService should allow
10% more requests)
6. SLA information is changed quite rarely and SlaService is quite
costly to call (~250ms per request), so consider caching SLA
requests. Also, you should not query the service if the same token
request is already in progress.
7. Consider that REST service average response time is below 5ms,
ThrottlingService shouldn’t impact REST service SLA.
