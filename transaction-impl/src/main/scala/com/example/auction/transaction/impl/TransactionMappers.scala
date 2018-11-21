package com.example.auction.transaction.impl

import com.example.auction.transaction.api.{DeliveryInfo, OfflinePaymentInfo, PaymentInfo, TransactionInfo}

object TransactionMappers {

  def toApiDelivery(data: Option[DeliveryData]): Option[DeliveryInfo] = {
    data.map { deliveryData =>
      DeliveryInfo(
        deliveryData.addressLine1,
        deliveryData.addressLine2,
        deliveryData.city,
        deliveryData.state,
        deliveryData.postalCode,
        deliveryData.country
      )
    }
  }

  def toApiPayment(data: Option[Payment]): Option[PaymentInfo] = {
    data.flatMap {
      case OfflinePayment(comment) => Some(OfflinePaymentInfo(comment))
      case _ => None
    }
  }

  def toApi(data: TransactionState): TransactionInfo = {
    // TransactionEntity verifies if a transaction in TransactionState is set
    // This code is called after this verification was done from TransactionServiceImpl
    // We can get() safely
    val transaction = data.transaction.get
    TransactionInfo(
      transaction.itemId,
      transaction.creator,
      transaction.winner,
      transaction.itemData,
      transaction.itemPrice,
      toApiDelivery(transaction.deliveryData),
      transaction.deliveryPrice,
      toApiPayment(transaction.payment),
      TransactionStatus.transactionInfoStatus(data.status)
    )
  }

}
