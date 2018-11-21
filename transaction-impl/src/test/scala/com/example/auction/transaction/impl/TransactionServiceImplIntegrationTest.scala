package com.example.auction.transaction.impl

import java.time.{Duration, Instant}
import java.util.UUID

import com.example.auction.item.api.ItemStatus.Status
import com.example.auction.item.api._
import com.example.auction.security.ClientSecurity._
import com.example.auction.transaction.api.{DeliveryInfo, TransactionInfo, TransactionInfoStatus, TransactionService}
import com.lightbend.lagom.scaladsl.api.AdditionalConfiguration
import com.lightbend.lagom.scaladsl.api.transport.NotFound
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.{ProducerStub, ProducerStubFactory, ServiceTest}
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import play.api.Configuration

import scala.concurrent.Await
import scala.concurrent.duration._

class TransactionServiceImplIntegrationTest extends WordSpec with Matchers with Eventually with ScalaFutures with BeforeAndAfterAll {

  val awaitTimeout = 2.seconds
  var itemProducerStub: ProducerStub[ItemEvent] = _

  val server = ServiceTest.startServer(ServiceTest.defaultSetup.withCassandra(true)) { ctx =>
    new TransactionApplication(ctx) with LocalServiceLocator {

      val stubFactory = new ProducerStubFactory(actorSystem, materializer)
      itemProducerStub = stubFactory.producer[ItemEvent]("item-ItemEvent")

      override lazy val itemService = new ItemStub(itemProducerStub)

      override def additionalConfiguration: AdditionalConfiguration =
        super.additionalConfiguration ++ Configuration.from(Map(
          "cassandra-query-journal.eventual-consistency-delay" -> "0",
          "lagom.circuit-breaker.default.enabled" -> "off"
        ))
    }
  }

  val transactionService = server.serviceClient.implement[TransactionService]

  override def afterAll = server.stop()

  val itemId = UUID.randomUUID()
  val creatorId = UUID.randomUUID()
  val winnerId = UUID.randomUUID()
  val itemPrice = 5000
  val itemData = ItemData("title", "desc", "EUR", 1, 10, Duration.ofMinutes(10), None)
  val item = Item(Some(itemId), creatorId, itemData, Some(itemPrice), ItemStatus.Completed, Some(Instant.now), Some(Instant.now), Some(winnerId))
  val auctionFinished = AuctionFinished(itemId, item)

  val deliveryInfo = DeliveryInfo("ADDR1", "ADDR2", "CITY", "STATE", 27, "COUNTRY")

  val transactionInfoStarted = TransactionInfo(itemId, creatorId, winnerId, itemData, itemPrice, None, None, None, TransactionInfoStatus.NegotiatingDelivery)
  val transactionInfoWithDeliveryInfo = new TransactionInfo(itemId, creatorId, winnerId, itemData, item.price.get, Some(deliveryInfo), None, None, TransactionInfoStatus.NegotiatingDelivery);

  "The Transaction service" should {

    "create transaction on auction finished" in {
      itemProducerStub.send(auctionFinished)
      eventually(timeout(Span(10, Seconds))) {
        retrieveTransaction(itemId, creatorId) should ===(transactionInfoStarted)
      }
    }

    "not create transaction with no winner" in {
      val itemIdWithNoWinner = UUID.randomUUID()
      val itemWithNoWinner = Item(Some(itemIdWithNoWinner), creatorId, itemData, Some(itemPrice), ItemStatus.Completed, Some(Instant.now()), Some(Instant.now()), None)
      val auctionFinishedWithNoWinner = AuctionFinished(itemIdWithNoWinner, itemWithNoWinner)
      itemProducerStub.send(auctionFinishedWithNoWinner)
      a[NotFound] should be thrownBy retrieveTransaction(itemIdWithNoWinner, creatorId)
    }

    "submit delivery details" in {
      itemProducerStub.send(auctionFinished)
      submitDeliveryDetails(itemId, winnerId, deliveryInfo)
      eventually(timeout(Span(15, Seconds))) {
        retrieveTransaction(itemId, creatorId) should ===(transactionInfoWithDeliveryInfo)
      }
    }

  }

  private def retrieveTransaction(itemId: UUID, creatorId: UUID): TransactionInfo = {
    Await.result(
      transactionService.getTransaction(itemId)
        .handleRequestHeader(authenticate(creatorId))
        .invoke(),
      awaitTimeout
    )
  }

  private def submitDeliveryDetails(itemId: UUID, winnerId: UUID, deliveryInfo: DeliveryInfo) = {
    Await.result(
      transactionService.submitDeliveryDetails(itemId)
        .handleRequestHeader(authenticate(winnerId))
        .invoke(deliveryInfo),
      awaitTimeout
    )
  }



}

class ItemStub(itemProducerStub: ProducerStub[ItemEvent]) extends ItemService {

  override def createItem = ???

  override def startAuction(id: UUID) = ???

  override def getItem(id: UUID) = ???

  override def getItemsForUser(id: UUID, status: Status, page: Option[String]) = ???

  override def itemEvents = itemProducerStub.topic

}
