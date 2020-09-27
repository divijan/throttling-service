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
      val isAllowed = 1 <= rps
      if (isAllowed) rpsCounter.addOne(user -> (Instant.now(), 1))
      isAllowed
    } { case (beginningOfTime, count) =>
      val now = Instant.now
      val duration = Duration.between(beginningOfTime, now).toMillis
      val roundedDuration: Double = (duration / 1000.0 * 10).toLong / 10.0
      val adjustedDuration: Double = if (roundedDuration < 1) 1 else roundedDuration
      val isAllowed = (count + 1) <= rps * adjustedDuration
      if (isAllowed) rpsCounter.replace(user, (beginningOfTime, count + 1))
      isAllowed
    }

    token match {
      case None     => isRequestAllowedForUser("unauthorized", graceRps)
      case Some(tk) =>
        val sla = slaSrvFrontend.getSlaByToken(tk) getOrElse Sla("unauthorized", graceRps)
        isRequestAllowedForUser(sla.user, sla.rps)
    }
  }
}
