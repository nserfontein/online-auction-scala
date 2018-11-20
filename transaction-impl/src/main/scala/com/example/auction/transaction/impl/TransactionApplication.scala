package com.example.auction.transaction.impl

import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.server.{LagomApplication, LagomApplicationContext, LagomServerComponents}

abstract class TransactionApplication(context: LagomApplicationContext) extends LagomApplication(context) {

}

trait TransactionComponents extends LagomServerComponents
  with CassandraPersistenceComponents {

  override lazy val lagomServer = serverFor[TransactionService]


}
