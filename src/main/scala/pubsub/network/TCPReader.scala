package pubsub.network

import java.net.Socket

import pubsub.Client
import pubsub.collection.BoundedBuffer
import pubsub.command._

class TCPReader(id: Int, socket: Socket, buffer: BoundedBuffer[Command]) {
  val client = new Client(socket, id)
  val reader = new CommandReader(socket.getInputStream(), client)

  def read(): Unit = {
    client.sayHello()
    while(client.isConnected) {

      reader.fetchCommand() match {
        case c: EndOfClient =>
          buffer.put(c)
          client.close()
        case _: MalformedCommand =>
          client.invalidPreviousCommand()
        case command =>
          println(command)
          buffer.put(command)
      }
    }
  } 
}