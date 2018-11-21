package com.example.auction.transaction.impl

import com.example.auction.transaction.api.TransactionService
import com.lightbend.lagom.scaladsl.api.Descriptor
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.server.{LagomApplication, LagomApplicationContext, LagomApplicationLoader, LagomServerComponents}
import com.lightbend.rp.servicediscovery.lagom.scaladsl.LagomServiceLocatorComponents
import com.softwaremill.macwire._
import play.api.libs.ws.ahc.AhcWSComponents

import scala.concurrent.ExecutionContext

abstract class TransactionApplication(context: LagomApplicationContext) extends LagomApplication(context)
  with TransactionComponents
  with AhcWSComponents {


}

trait TransactionComponents extends LagomServerComponents
  with CassandraPersistenceComponents {

  implicit def executionContext: ExecutionContext

  override lazy val lagomServer = serverFor[TransactionService](wire[TransactionServiceImpl])
  lazy val jsonSerializerRegistry = TransactionSerializerRegistry
  lazy val transactionRepository = wire[TransactionRepository]

  persistentEntityRegistry.register(wire[TransactionEntity])
  readSide.register(wire[TransactionEventProcessor])

}

class TransactionApplicationLoader extends LagomApplicationLoader {

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new TransactionApplication(context) with LagomDevModeComponents

  override def load(context: LagomApplicationContext): LagomApplication =
    new TransactionApplication(context) with LagomServiceLocatorComponents

  override def describeService: Option[Descriptor] = Some(readDescriptor[TransactionService])

}
