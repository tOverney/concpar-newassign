package main.scala

import java.net.Socket
import java.nio.charset.Charset

case class Client(socket: Socket, id: Int)
    (implicit charset: Charset = Charset.forName("UTF-8")) {
  val name = "client " + id
  val outStream = socket.getOutputStream()

  def isConnected: Boolean = socket.isConnected()

  def close(): Unit = socket.close()

  def send(message: String): Unit = {
    val payload = message.getBytes(charset)
    outStream.write(payload)
    outStream.flush()
  }

  def sendAck(ackType: String, message: String): Unit =
    send(s"${ackType}_ack $message\n")

  def sayHello(): Unit = sendAck("connection", name)

  def invalidPreviousCommand(): Unit = send("! previous command was invalid\n")

  def sendMessage(topic: String, message: String): Unit =
    send(s"$topic \'$message\'\n")
}