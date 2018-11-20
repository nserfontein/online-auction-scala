package com.example.auction.transaction.impl

import java.time.Duration
import java.util.UUID

import akka.actor.ActorSystem
import com.example.auction.item.api.ItemData
import com.lightbend.lagom.scaladsl.api.transport.Forbidden
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.testkit.PersistentEntityTestDriver
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

class TransactionEntitySpec extends WordSpec with Matchers with BeforeAndAfterAll {

  private val system = ActorSystem("TransactionEntitySpec", JsonSerializerRegistry.actorSystemSetupFor(TransactionSerializerRegistry))

  private val itemId = UUID.randomUUID()
  private val creator = UUID.randomUUID()
  private val winner = UUID.randomUUID()
  private val itemData = ItemData("title", "desc", "EUR", 1, 10, Duration.ofMinutes(10), None)
  private val deliveryData = DeliveryData("Addr1", "Addr2", "City", "State", 27, "Country")
  private val deliveryPrice = 500
  private val payment = OfflinePayment("Payment sent via wire transfer")

  private val transaction = Transaction(itemId, creator, winner, itemData, 2000)
  private val startTransaction = StartTransaction(transaction)
  private val submitDeliveryDetails = SubmitDeliveryDetails(winner, deliveryData)

  private def withTestDriver(block: PersistentEntityTestDriver[TransactionCommand, TransactionEvent, TransactionState] => Unit): Unit = {
    val driver = new PersistentEntityTestDriver(system, new TransactionEntity, itemId.toString)
    block(driver)
    if (driver.getAllIssues.nonEmpty) {
      driver.getAllIssues.foreach(println)
      fail(s"There were issues ${driver.getAllIssues.head}")
    }
  }

  "The transaction entity" should {

    "emit event when creating transaction" in withTestDriver { driver =>
      val outcome = driver.run(startTransaction)
      outcome.state.status should ===(TransactionStatus.NegotiatingDelivery)
      outcome.state.transaction should ===(Some(transaction))
      outcome.events should contain only TransactionStarted(itemId, transaction)
    }

    "emit event when submitting delivery details" in withTestDriver { driver =>
      driver.run(startTransaction)
      val outcome = driver.run(submitDeliveryDetails)
      outcome.state.status should ===(TransactionStatus.NegotiatingDelivery)
      outcome.events should contain only DeliveryDetailsSubmitted(itemId, deliveryData)
    }

    "forbid submitting delivery details by non-buyer" in withTestDriver { driver =>
      driver.run(startTransaction)
      val hacker = UUID.randomUUID()
      val invalid = SubmitDeliveryDetails(hacker, deliveryData)
      a[Forbidden] should be thrownBy driver.run(invalid)
    }

  }

}
