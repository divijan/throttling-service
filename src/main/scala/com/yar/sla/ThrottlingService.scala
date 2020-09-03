package com.yar.sla

import java.time.{Duration, Instant}
import java.util.concurrent.{ConcurrentHashMap, ConcurrentMap, TimeoutException}

import com.typesafe.config.ConfigFactory

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.jdk.CollectionConverters._


trait ThrottlingService {
  val graceRps: Int // configurable
  val slaService: SlaService // use mocks/stubs for testing
  // Should return true if the request is within allowed RPS.
  def isRequestAllowed(token: Option[String]): Boolean
}

object MockThrottlingService extends ThrottlingService {
  override val graceRps: Int = ConfigFactory.load().getInt("graceRps")
  override val slaService: SlaService = new SlaService {
    val table = Map(
      "tk2" -> Sla("John", 30),
      "tk1" -> Sla("Chris", 15),
      "tk3" -> Sla("John", 30)
    )

    override def getSlaByToken(token: String): Future[Sla] = {
      Future {
        Thread.sleep(250)
        table(token)
      }// no requirements what to do if no Sla found for token, so we just fail inside the Future
    }
  }

  private val slaSrvFrontend = new SlaServiceFrontend(slaService)

  val rpsCounter = new ConcurrentHashMap[String, (Instant, Int)]().asScala

  override def isRequestAllowed(token: Option[String]): Boolean = {
    def isRequestAllowedByUser(user: String, rps: Int) = rpsCounter.get(user).fold {
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
      case None     => isRequestAllowedByUser("unauthorized", graceRps)
      case Some(tk) =>
        val optSla = try {
          slaSrvFrontend.getSlaByToken(tk)
        } catch {
          case e: TimeoutException => Some(Sla("unauthorized", graceRps))
        }
        optSla.fold(isRequestAllowedByUser("unauthorized", graceRps))(sla => isRequestAllowedByUser(sla.user, sla.rps))
    }
  }
}
