package main.scala

class CommandHandler(buffer: ProducerConsumer[Command]) {
  import CommandHandler._
  
  def handle(): Unit = {
    val command = buffer.take()

    command match {
      case Subscribe(topic, client) =>
        multiMap.add(topic, client)
        client.sendAck("subscribe", topic)

      case Unsubscribe(topic, client) =>
        multiMap.remove(topic, client)
        client.sendAck("unsubscribe", topic)

      case Publish(topic, message, _) =>
        for {
          subscribers <- multiMap.get(topic)
          client <- subscribers
        } client.sendMessage(topic, message)

      case EndOfClient(client) =>
        multiMap.removeValueFromAll(client)

      case _ =>
        // nothing should happen
    }
  }
}


object CommandHandler {
  val multiMap = new ConcurrentMultiMap[String, Client]()
}
