package com.example.auction.transaction.impl

import java.util.UUID

import akka.{Done, NotUsed}
import com.example.auction.transaction.api._
import com.lightbend.lagom.scaladsl.api.ServiceCall

class TransactionServiceImpl extends TransactionService {

  override def submitDeliveryDetails(itemId: UUID): ServiceCall[DeliveryInfo, Done] = ???

  override def setDeliveryPrice(itemId: UUID): ServiceCall[Int, Done] = ???

  override def approveDeliveryDetails(itemId: UUID): ServiceCall[NotUsed, Done] = ???

  override def submitPaymentDetails(itemId: UUID): ServiceCall[PaymentInfo, Done] = ???

  override def submitPaymentStatus(itemId: UUID): ServiceCall[PaymentInfoStatus.Status, Done] = ???

  override def dispatchItem(itemId: UUID): ServiceCall[NotUsed, Done] = ???

  override def receiveItem(itemId: UUID): ServiceCall[NotUsed, Done] = ???

  override def initiateRefund(itemId: UUID): ServiceCall[NotUsed, Done] = ???

  override def getTransaction(itemId: UUID): ServiceCall[NotUsed, TransactionInfo] = ???

  override def getTransactionsForUser(status: TransactionInfoStatus.Status, pageNo: Option[Int], pageSize: Option[Int]): ServiceCall[NotUsed, Seq[TransactionSummary]] = ???

}
