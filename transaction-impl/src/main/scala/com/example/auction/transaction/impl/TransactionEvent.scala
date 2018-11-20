package com.example.auction.transaction.impl

import java.util.UUID

import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag, AggregateEventTagger}
import play.api.libs.json.{Format, Json}

trait TransactionEvent extends AggregateEvent[TransactionEvent] {
  override def aggregateTag: AggregateEventTagger[TransactionEvent] = TransactionEvent.Tag
}

object TransactionEvent {
  val NumShards = 4
  val Tag = AggregateEventTag.sharded[TransactionEvent](NumShards)
}

case class TransactionStarted(itemId: UUID, transaction: Transaction) extends TransactionEvent

object TransactionStarted {
  implicit val format: Format[TransactionStarted] = Json.format
}

case class DeliveryDetailsSubmitted(itemId: UUID, deliveryData: DeliveryData) extends TransactionEvent

object DeliveryDetailsSubmitted {
  implicit val format: Format[DeliveryDetailsSubmitted] = Json.format
}

case class DeliveryPriceUpdated(itemId: UUID, deliveryPrice: Int) extends TransactionEvent

object DeliveryPriceUpdated {
  implicit val format: Format[DeliveryPriceUpdated] = Json.format
}