package com.example.auction.transaction.impl

import java.util.UUID

import akka.Done
import com.lightbend.lagom.scaladsl.api.transport.Forbidden
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity

class TransactionEntity extends PersistentEntity {

  override type State = TransactionState
  override type Command = TransactionCommand
  override type Event = TransactionEvent

  override def initialState: TransactionState = TransactionState.notStarted

  override def behavior: Behavior = {
    case TransactionState(_, TransactionStatus.NotStarted) => notStarted
    case TransactionState(_, TransactionStatus.NegotiatingDelivery) => negotiatingDelivery
    // TODO: Complete
  }

  private def getTransactionHandler = {
    Actions().onReadOnlyCommand[GetTransaction.type, TransactionState] {
      case (GetTransaction, ctx, state) => ctx.reply(state)
    }
  }

  private val notStarted = {
    Actions().onCommand[StartTransaction, Done] {
      case (StartTransaction(transaction), ctx, _) =>
        ctx.thenPersist(TransactionStarted(UUID.fromString(entityId), transaction))(_ => ctx.reply(Done))
    }.onEvent {
      case (TransactionStarted(_, transaction), _) =>
        TransactionState.start(transaction)
    }
      .orElse(getTransactionHandler)
  }

  private val negotiatingDelivery = {
    Actions().onReadOnlyCommand[StartTransaction, Done] {
      case (StartTransaction(_), ctx, _) => ctx.reply(Done)
    }.onCommand[SubmitDeliveryDetails, Done] {
      case (SubmitDeliveryDetails(userId, deliveryData), ctx, state) =>
        if (userId == state.transaction.get.winner) {
          ctx.thenPersist(DeliveryDetailsSubmitted(UUID.fromString(entityId), deliveryData))(_ => ctx.reply(Done))
        } else {
          throw Forbidden("Only the auction winner can submit delivery details")
        }
    }.onCommand[SetDeliveryPrice, Done] {
      case (SetDeliveryPrice(userId, deliveryPrice), ctx, state) =>
        if (userId == state.transaction.get.creator) {
          ctx.thenPersist(DeliveryPriceUpdated(UUID.fromString(entityId), deliveryPrice))(_ => ctx.reply(Done))
        } else {
          throw Forbidden("Only the item creator can set the delivery price")
        }
    }.onEvent {
      case (DeliveryDetailsSubmitted(itemId, deliveryData), state) =>
        state.updateDeliveryData(deliveryData)
    }.onEvent {
      case (DeliveryPriceUpdated(_, deliveryPrice), state) =>
        state.updateDeliveryPrice(deliveryPrice)
    }
      .orElse(getTransactionHandler)
    // TODO: Complete
  }

}
