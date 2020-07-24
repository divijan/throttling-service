package com.yar.sla

import scala.concurrent.Future

case class Sla(user: String, rps: Int)

trait SlaService {
  def getSlaByToken(token: String): Future[Sla]
}

