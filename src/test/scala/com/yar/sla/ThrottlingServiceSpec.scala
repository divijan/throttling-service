package com.yar.sla

import org.scalatest.flatspec.{AnyFlatSpec, AsyncFlatSpec}
import org.scalatest.matchers.should.Matchers
import org.scalamock.scalatest.MockFactory
//import org.scalamock.scalatest.proxy.AsyncMockFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps
import scala.concurrent.Future

class MyThrottlingServiceSpec extends AnyFlatSpec with Matchers with MockFactory {

  "MyThrottlingService" should "only query slaService once for the same token requests" in {
    val slaServiceMock = mock[SlaService]
    val ts = new MyThrottlingService(2, slaServiceMock)
    (slaServiceMock.getSlaByToken _).expects("Hans") onCall { arg: String => Future {
      Thread.sleep(250)
      Sla("Hans", 15)
    }} once;
    ts.isRequestAllowed(Some("Hans"))
    ts.isRequestAllowed(Some("Hans")) shouldEqual true
  }

  it should "use grace RPS for yet unauthorized requests" in {
    val slaServiceMock = mock[SlaService] //making these two class fields makes this test fail
    val ts = new MyThrottlingService(2, slaServiceMock)
    (slaServiceMock.getSlaByToken _).expects("Peter") onCall { arg: String => Future {
      Thread.sleep(250)
      Sla("Peter", 15)
    }} once;
    ts.isRequestAllowed(Some("Peter")) shouldEqual true
    ts.isRequestAllowed(Some("Peter")) shouldEqual true
    ts.isRequestAllowed(Some("Peter")) shouldEqual false
  }

  it should "use grace RPS for no token requests" in {
    val slaServiceMock = mock[SlaService] //moving these two lines to the class makes this test fail
    val ts = new MyThrottlingService(2, slaServiceMock)
    (slaServiceMock.getSlaByToken _).expects(*).never;
    ts.isRequestAllowed(None) shouldEqual true
    ts.isRequestAllowed(None) shouldEqual true
    ts.isRequestAllowed(None) shouldEqual false
  }

  it should "cache SLAs received from SLA service" in {
    val slaServiceMock = mock[SlaService] //moving these two lines to the class makes this test fail
    val ts = new MyThrottlingService(2, slaServiceMock)
    (slaServiceMock.getSlaByToken _).expects("Hans") onCall { arg: String => Future {
      Thread.sleep(100)
      Sla("Hans", 1)
    }} once;
    ts.isRequestAllowed(Some("Hans")) shouldEqual true
    Thread.sleep(100)
    ts.isRequestAllowed(Some("Hans")) shouldEqual true
    ts.isRequestAllowed(Some("Hans")) shouldEqual false
  }

  it should "have granularity of 0.1 s" in {
    val slaServiceMock = mock[SlaService] //moving these two lines to the class makes this test fail
    val ts = new MyThrottlingService(2, slaServiceMock)
    (slaServiceMock.getSlaByToken _).expects("Hans") onCall { arg: String => Future {
      Sla("Hans", 10)
    }} once;
    val now = System.currentTimeMillis()
    for (_ <- 1 to 10) yield {
      ts.isRequestAllowed(Some("Hans")) shouldEqual true
    }
    println(s"${System.currentTimeMillis() - now} millis elapsed")
    Thread.sleep(100)
    ts.isRequestAllowed(Some("Hans")) shouldEqual false //not enough time elapsed
  }
}
