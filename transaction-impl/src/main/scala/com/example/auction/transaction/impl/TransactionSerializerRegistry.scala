package com.example.auction.transaction.impl

import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}

object TransactionSerializerRegistry extends JsonSerializerRegistry {

  override def serializers = List(
    // State
    JsonSerializer[TransactionState],
    // Commands and replies
    JsonSerializer[StartTransaction],
    // Events
    JsonSerializer[TransactionStarted]
  )

}
