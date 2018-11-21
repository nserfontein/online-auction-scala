package com.example.auction.transaction.impl

import java.time.Duration
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

import akka.persistence.query.Sequence
import com.datastax.driver.core.utils.UUIDs
import com.example.auction.item.api.ItemData
import com.example.auction.transaction.api.{TransactionInfoStatus, TransactionSummary}
import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.server.LagomApplication
import com.lightbend.lagom.scaladsl.testkit.{ReadSideTestDriver, ServiceTest}
import org.scalatest.{AsyncWordSpec, BeforeAndAfterAll, Matchers}
import play.api.libs.ws.ahc.AhcWSComponents

class TransactionRepositorySpec extends AsyncWordSpec with BeforeAndAfterAll with Matchers {

  private val server = ServiceTest.startServer(ServiceTest.defaultSetup.withCassandra(true)) { ctx =>
    new LagomApplication(ctx) with TransactionComponents with AhcWSComponents with LagomKafkaComponents {
      override def serviceLocator = NoServiceLocator
      override lazy val readSide: ReadSideTestDriver = new ReadSideTestDriver
    }
  }

  override def afterAll() = server.stop()

  private val testDriver = server.application.readSide
  private val transactionRepository = server.application.transactionRepository
  private val offset = new AtomicInteger()

  private val itemId = UUIDs.timeBased()
  private val creatorId = UUID.randomUUID
  private val winnerId = UUID.randomUUID
  private val itemTitle = "title"
  private val currencyId = "EUR"
  private val itemPrice = 2000
  private val itemData = ItemData(itemTitle, "desc", currencyId, 1, 10, Duration.ofMinutes(10), None)
  private val transaction =  Transaction(itemId, creatorId, winnerId, itemData, itemPrice)

  private val deliveryData = DeliveryData("Addr1", "Addr2", "City", "State", 27, "Country")
  private val deliveryPrice = 500
  private val payment = OfflinePayment("Payment sent via wire transfer")

  "The transaction repository" should {

    "get transaction started for creator" in {
      shouldGetTransactionStarted(creatorId)
    }

    "get transaction started for winner" in {
      shouldGetTransactionStarted(winnerId)
    }

    "update status to payment pending for creator" in {
      shouldUpdateStatusToPaymentPending(creatorId)
    }

    "update status to payment pending for winner" in {
      shouldUpdateStatusToPaymentPending(winnerId)
    }

  }

  private def shouldGetTransactionStarted(userId: UUID) = {
    for {
      _ <- feed(TransactionStarted(itemId, transaction))
      transactions <- getTransactions(userId, TransactionInfoStatus.NegotiatingDelivery)
    } yield {
      transactions.count should ===(1)
      val expected = new TransactionSummary(itemId, creatorId, winnerId, itemTitle, currencyId, itemPrice, TransactionInfoStatus.NegotiatingDelivery)
      transactions.items.head should ===(expected)
    }
  }

  private def shouldUpdateStatusToPaymentPending(userId: UUID) = for {
    _ <- feed(TransactionStarted(itemId, transaction))
    _ <- feed(DeliveryDetailsSubmitted(itemId, deliveryData))
    _ <- feed(DeliveryPriceUpdated(itemId, deliveryPrice))
    _ <- feed(DeliveryDetailsApproved(itemId))
    transactions <- getTransactions(userId, TransactionInfoStatus.PaymentPending)
  } yield {
    transactions.count should ===(1)
    val expected = TransactionSummary(itemId, creatorId, winnerId, itemTitle, currencyId, itemPrice, TransactionInfoStatus.PaymentPending)
    transactions.items.head should ===(expected)
  }

  private def getTransactions(userId: UUID, transactionStatus: TransactionInfoStatus.Status) = {
    transactionRepository.getTransactionsForUser(userId, transactionStatus, 0, 10)
  }

  private def feed(event: TransactionEvent) = {
    testDriver.feed(event.itemId.toString, event, Sequence(offset.getAndIncrement))
  }

}
