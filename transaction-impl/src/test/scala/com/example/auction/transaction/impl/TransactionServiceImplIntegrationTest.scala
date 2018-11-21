package com.example.auction.transaction.impl

import java.time.{Duration, Instant}
import java.util.UUID

import com.example.auction.item.api.ItemStatus.Status
import com.example.auction.item.api._
import com.example.auction.security.ClientSecurity._
import com.example.auction.transaction.api.{TransactionInfo, TransactionInfoStatus, TransactionService}
import com.lightbend.lagom.scaladsl.api.AdditionalConfiguration
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.{ProducerStub, ProducerStubFactory, ServiceTest, TestTopicComponents}
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import play.api.Configuration

import scala.concurrent.Future

class TransactionServiceImplIntegrationTest extends WordSpec with Matchers with Eventually with ScalaFutures with BeforeAndAfterAll {

  private var itemProducerStub: ProducerStub[ItemEvent] = _

  private val server = ServiceTest.startServer(ServiceTest.defaultSetup.withCassandra(true)) { ctx =>
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

  private val transactionService = server.serviceClient.implement[TransactionService]

  override def afterAll = server.stop()

  private val itemId = UUID.randomUUID()
  private val creatorId = UUID.randomUUID()
  private val winnerId = UUID.randomUUID()
  private val itemPrice = 5000
  private val itemData = ItemData("title", "desc", "EUR", 1, 10, Duration.ofMinutes(10), None)
  private val item = Item(Some(itemId), creatorId, itemData, Some(itemPrice), ItemStatus.Completed, Some(Instant.now), Some(Instant.now), Some(winnerId))
  private val auctionFinished = AuctionFinished(itemId, item)

  private val transactionInfoStarted = TransactionInfo(itemId, creatorId, winnerId, itemData, itemPrice, None, None, None, TransactionInfoStatus.NegotiatingDelivery)

  "The Transaction service" should {

    "create transaction on auction finished" in {
      itemProducerStub.send(auctionFinished)
      eventually(timeout(Span(10, Seconds))) {
        val retrievedTransaction = retrieveTransaction(itemId, creatorId)
        whenReady(retrievedTransaction) { resp =>
          resp should ===(transactionInfoStarted)
        }
      }
    }

  }

  private def retrieveTransaction(itemId: UUID, creatorId: UUID): Future[TransactionInfo] = {
    transactionService.getTransaction(itemId)
      .handleRequestHeader(authenticate(creatorId))
      .invoke()
  }

}

private class ItemStub(itemProducerStub: ProducerStub[ItemEvent]) extends ItemService {

  override def createItem = ???

  override def startAuction(id: UUID) = ???

  override def getItem(id: UUID) = ???

  override def getItemsForUser(id: UUID, status: Status, page: Option[String]) = ???

  override def itemEvents = itemProducerStub.topic

}
