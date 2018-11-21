package com.example.auction.transaction.impl

import java.time.{Duration, Instant}
import java.util.UUID

import com.datastax.driver.core.utils.UUIDs
import com.example.auction.item.api.ItemStatus.Status
import com.example.auction.item.api._
import com.example.auction.security.ClientSecurity._
import com.example.auction.transaction.api._
import com.lightbend.lagom.scaladsl.api.AdditionalConfiguration
import com.lightbend.lagom.scaladsl.api.transport.NotFound
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.{ProducerStub, ProducerStubFactory, ServiceTest}
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, Matchers, OneInstancePerTest, WordSpec}
import play.api.Configuration

import scala.concurrent.Await
import scala.concurrent.duration._

class TransactionServiceImplIntegrationTest extends WordSpec with Matchers with Eventually with ScalaFutures with BeforeAndAfterAll {

  val awaitTimeout = 10.seconds
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

  // TODO: Use loan
  def fixture = new {
    val itemId = UUIDs.timeBased()
    val creatorId = UUID.randomUUID()
    val winnerId = UUID.randomUUID()
    val itemPrice = 5000
    val itemData = ItemData("title", "desc", "EUR", 1, 10, Duration.ofMinutes(10), None)
    val item = Item(Some(itemId), creatorId, itemData, Some(itemPrice), ItemStatus.Completed, Some(Instant.now), Some(Instant.now), Some(winnerId))
    val auctionFinished = AuctionFinished(itemId, item)

    val deliveryInfo = DeliveryInfo("ADDR1", "ADDR2", "CITY", "STATE", 27, "COUNTRY")
    val deliveryPrice = 500
    val paymentInfo = OfflinePaymentInfo("Payment sent via wire transfer")

    val transactionInfoStarted = TransactionInfo(itemId, creatorId, winnerId, itemData, itemPrice, None, None, None, TransactionInfoStatus.NegotiatingDelivery)
    val transactionInfoWithDeliveryInfo = TransactionInfo(itemId, creatorId, winnerId, itemData, item.price.get, Some(deliveryInfo), None, None, TransactionInfoStatus.NegotiatingDelivery)
    val transactionInfoWithDeliveryPrice = TransactionInfo(itemId, creatorId, winnerId, itemData, item.price.get, None, Some(deliveryPrice), None, TransactionInfoStatus.NegotiatingDelivery)
  }

  "The Transaction service" should {

    "create transaction on auction finished" in {
      val f = fixture
      itemProducerStub.send(f.auctionFinished)
      eventually(timeout(Span(10, Seconds))) {
        retrieveTransaction(f.itemId, f.creatorId) should ===(f.transactionInfoStarted)
      }
    }

    "not create transaction with no winner" in {
      val f = fixture
      val itemIdWithNoWinner = UUID.randomUUID()
      val itemWithNoWinner = Item(Some(itemIdWithNoWinner), f.creatorId, f.itemData, Some(f.itemPrice), ItemStatus.Completed, Some(Instant.now()), Some(Instant.now()), None)
      val auctionFinishedWithNoWinner = AuctionFinished(itemIdWithNoWinner, itemWithNoWinner)
      itemProducerStub.send(auctionFinishedWithNoWinner)
      a[NotFound] should be thrownBy retrieveTransaction(itemIdWithNoWinner, f.creatorId)
    }

    "submit delivery details" in {
      val f = fixture
      itemProducerStub.send(f.auctionFinished)
      submitDeliveryDetails(f.itemId, f.winnerId, f.deliveryInfo)
      eventually(timeout(Span(15, Seconds))) {
        retrieveTransaction(f.itemId, f.creatorId) should ===(f.transactionInfoWithDeliveryInfo)
      }
    }

    "set delivery price" in {
      val f = fixture
      itemProducerStub.send(f.auctionFinished)
      setDeliveryPrice(f.itemId, f.creatorId, f.deliveryPrice)
      eventually(timeout(Span(15, Seconds))) {
        retrieveTransaction(f.itemId, f.creatorId) should ===(f.transactionInfoWithDeliveryPrice)
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

  private def setDeliveryPrice(itemId: UUID, creatorId: UUID, deliveryPrice: Int) = {
    Await.result(
      transactionService.setDeliveryPrice(itemId)
        .handleRequestHeader(authenticate(creatorId))
        .invoke(deliveryPrice),
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
