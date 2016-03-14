package main.scala

import java.net.Socket

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
          buffer.put(command)
      }
    }
  } 
}