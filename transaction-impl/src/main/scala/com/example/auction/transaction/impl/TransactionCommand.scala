package com.example.auction.transaction.impl

import java.util.UUID

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import com.lightbend.lagom.scaladsl.playjson.JsonSerializer
import play.api.libs.json.{Format, Json}

trait TransactionCommand

case class StartTransaction(transaction: Transaction) extends TransactionCommand with ReplyType[Done]

object StartTransaction {
  implicit val format: Format[StartTransaction] = Json.format
}

case class SubmitDeliveryDetails(userId: UUID, deliveryData: DeliveryData) extends TransactionCommand with ReplyType[Done]

object SubmitDeliveryDetails {
  implicit val format: Format[SubmitDeliveryDetails] = Json.format
}

case object GetTransaction extends TransactionCommand with ReplyType[TransactionState] {
  implicit val format: Format[GetTransaction.type] = JsonSerializer.emptySingletonFormat(GetTransaction)
}
