package com.yar.sla

import java.util.concurrent.TimeoutException
import scalacache.caffeine.CaffeineCache
import scalacache.{Cache, sync}
import scalacache.modes.sync._

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._


case class Sla(user: String, rps: Int)

trait SlaService {
  def getSlaByToken(token: String): Future[Sla]
}

class SlaServiceFrontend(slaService: SlaService) {
  // Option in case there is no entry for a certain token in SlaService
  implicit val slaCache: Cache[Future[Sla]] = CaffeineCache[Future[Sla]]

  def getSlaByToken(token: String)(implicit ec: ExecutionContext): Option[Sla] = {
    val futureSla = sync.get(token).getOrElse {
      val slaFromService = slaService.getSlaByToken(token)
      slaCache.put(token)(slaFromService, Some(1.day))
      slaFromService
    }
    try {
      Await.result(futureSla.map(Some(_)).recover { case _: NoSuchElementException => None }, 4.millis)
    } catch {
      case e: TimeoutException => None
    }
  }
}