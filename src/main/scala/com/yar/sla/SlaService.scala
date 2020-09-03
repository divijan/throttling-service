package com.yar.sla

import scalacache.caffeine.CaffeineCache
import scalacache.{Cache, sync}
import scalacache.modes.sync._

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

case class Sla(user: String, rps: Int)

trait SlaService {
  def getSlaByToken(token: String): Future[Sla]
}

class SlaServiceFrontend(slaService: SlaService) {
  // Option in case there is no entry for a certain token in SlaService
  implicit val slaCache: Cache[Option[Sla]] = CaffeineCache[Option[Sla]]
  val requestsInProgress = new java.util.concurrent.ConcurrentHashMap[String, Future[Sla]].asScala

  def getSlaByToken(token: String)(implicit ec: ExecutionContext): Option[Sla] =
    sync.get(token).getOrElse {
      val futureSla = requestsInProgress.getOrElse(token, {
        val slaFromService = slaService.getSlaByToken(token)
        requestsInProgress.addOne(token -> slaFromService)
        slaFromService.onComplete { result =>
          requestsInProgress.remove(token)
          slaCache.put(token)(result.toOption, Some(1.day))
        }
        slaFromService
      })
      Await.result(futureSla.map(Some(_)).recover { case _: NoSuchElementException => None }, 4.millis)
    }
}