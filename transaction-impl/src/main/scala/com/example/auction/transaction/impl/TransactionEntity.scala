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
    case TransactionState(_, TransactionStatus.PaymentPending) => paymentPending
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
    }.onCommand[ApproveDeliveryDetails, Done] {
      case (ApproveDeliveryDetails(userId), ctx, state) =>
        if (userId == state.transaction.get.creator) {
          if (state.transaction.get.deliveryData.isDefined && state.transaction.get.deliveryPrice.isDefined) {
            ctx.thenPersist(DeliveryDetailsApproved(UUID.fromString(entityId)))(_ => ctx.reply(Done))
          } else {
            throw Forbidden("Can't approve empty delivery detail")
          }
        } else {
          throw Forbidden("Only the item creator can approve the delivery details")
        }
    }.onEvent {
      case (DeliveryDetailsSubmitted(_, deliveryData), state) =>
        state.updateDeliveryData(deliveryData)
    }.onEvent {
      case (DeliveryPriceUpdated(_, deliveryPrice), state) =>
        state.updateDeliveryPrice(deliveryPrice)
    }.onEvent {
      case (DeliveryDetailsApproved(_), state) =>
        state.withStatus(TransactionStatus.PaymentPending)
    }
      .orElse(getTransactionHandler)
    // TODO: Complete
  }

  private val paymentPending = {
    Actions().onCommand[SubmitPaymentDetails, Done] {
      case (SubmitPaymentDetails(userId, payment), ctx, state) =>
        if (userId == state.transaction.get.winner) {
          ctx.thenPersist(PaymentDetailsSubmitted(UUID.fromString(entityId), payment))(_ => ctx.reply(Done))
        } else {
          throw Forbidden("Only the auction winner can submit payment details")
        }
    }.onEvent {
      case (PaymentDetailsSubmitted(_, payment), state) =>
        state.updatePayment(payment).withStatus(TransactionStatus.PaymentSubmitted)
    } // TODO: Handle DeliverDetailsApproved event?
      .orElse(getTransactionHandler)
  }

}
