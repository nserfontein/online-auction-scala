package com.example.auction.transaction.impl

import java.util.UUID

import akka.Done
import com.example.auction.utils.JsonFormats.enumFormat
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

case class SetDeliveryPrice(userId: UUID, deliveryPrice: Int) extends TransactionCommand with ReplyType[Done]

object SetDeliveryPrice {
  implicit val format: Format[SetDeliveryPrice] = Json.format
}

case class ApproveDeliveryDetails(userId: UUID) extends TransactionCommand with ReplyType[Done]

object ApproveDeliveryDetails {
  implicit val format: Format[ApproveDeliveryDetails] = Json.format
}

case class SubmitPaymentDetails(userId: UUID, payment: Payment) extends TransactionCommand with ReplyType[Done]

object SubmitPaymentDetails {
  implicit val format: Format[SubmitPaymentDetails] = Json.format
}

object PaymentStatus extends Enumeration {
  val Approved, Rejected = Value
  type Status = Value
  implicit val format: Format[Status] = enumFormat(PaymentStatus)
}

case class SubmitPaymentStatus(userId: UUID, paymentStatus: PaymentStatus.Status) extends TransactionCommand with ReplyType[Done]

object SubmitPaymentStatus {
  implicit val format: Format[SubmitPaymentStatus] = Json.format
}












