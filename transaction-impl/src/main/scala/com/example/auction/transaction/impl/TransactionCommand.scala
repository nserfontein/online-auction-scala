package com.example.auction.transaction.impl

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import com.lightbend.lagom.scaladsl.playjson.JsonSerializer
import play.api.libs.json.{Format, Json}

trait TransactionCommand

case class StartTransaction(transaction: Transaction) extends TransactionCommand with ReplyType[Done]

object StartTransaction {
  implicit val format: Format[StartTransaction] = Json.format
}

case object GetTransaction extends TransactionCommand with ReplyType[TransactionState] {
  implicit val format: Format[GetTransaction.type] = JsonSerializer.emptySingletonFormat(GetTransaction)
}
