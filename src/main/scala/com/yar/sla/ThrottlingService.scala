package com.yar.sla

import java.time.{Duration, Instant}
import java.util.concurrent.ConcurrentHashMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.jdk.CollectionConverters._


trait ThrottlingService {
  val graceRps: Int // configurable
  val slaService: SlaService // use mocks/stubs for testing
  // Should return true if the request is within allowed RPS.
  def isRequestAllowed(token: Option[String]): Boolean
}

class MyThrottlingService(val graceRps: Int, val slaService: SlaService) extends ThrottlingService {
  private val slaSrvFrontend = new SlaServiceFrontend(slaService)
  private val rpsCounter = new ConcurrentHashMap[String, (Instant, Int)]().asScala


  override def isRequestAllowed(token: Option[String]): Boolean = {
    def isRequestAllowedForUser(user: String, rps: Int) = rpsCounter.get(user).fold {
      rpsCounter.addOne(user -> (Instant.now(), 1))
      1 <= rps
    } { tuple =>
      val (oldTimestamp, count) = tuple
      val now = Instant.now
      val duration = Duration.between(oldTimestamp, now).toMillis;
      if (duration >= 1000) {
        rpsCounter.replace(user, (now, 1))
        1 <= rps
      } else {
        val newCount = count + 1
        rpsCounter.replace(user, (oldTimestamp, newCount))
        count * (1 - duration/100/10.0) + 1 <= rps
      }
    }

    token match {
      case None     => isRequestAllowedForUser("unauthorized", graceRps)
      case Some(tk) =>
        val sla = slaSrvFrontend.getSlaByToken(tk) getOrElse Sla("unauthorized", graceRps)
        isRequestAllowedForUser(sla.user, sla.rps)
    }
  }
}
