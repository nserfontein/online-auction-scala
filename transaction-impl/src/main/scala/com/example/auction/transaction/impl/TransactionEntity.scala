package com.example.auction.transaction.impl

import java.util.UUID

import akka.Done
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
    Actions()
    // TODO: Complete
  }

}
