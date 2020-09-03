package com.yar.sla

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalamock.scalatest.MockFactory
import org.scalamock.scalatest.proxy.AsyncMockFactory
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

import scala.concurrent.Future

class MyThrottlingServiceSpec extends AnyFlatSpec with Matchers with MockFactory {
  val slaServiceMock = mock[SlaService]
  val ts = new MyThrottlingService(5, slaServiceMock)

  "MyThrottlingService" should "only query slaService once for the same token requests" in {
    (slaServiceMock.getSlaByToken _).expects("Hans") onCall { arg: String => Future {
      Thread.sleep(250)
      Sla("Hans", 15)
    }} once;
    ts.isRequestAllowed(Some("Hans"))
    ts.isRequestAllowed(Some("Hans")) shouldEqual true
  }
  //it should "query "
}
