package main.scala

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.InputStream

class CommandReader(inStream: InputStream, client: Client) {
  val inputBuffer = new BufferedReader(new InputStreamReader(inStream))

  def fetchCommand(): Command = {
    val line = inputBuffer.readLine()
    try {
      if (line == null) {
        EndOfClient(client)
      }
      else {
        val parts = line.split(" \\'")
        val Array(command, topic) = parts(0).split(" ")
        command match {
          case "subscribe"   => Subscribe(topic, client)
          case "unsubscribe" => Unsubscribe(topic, client)

          case "publish" => 
            var message = parts(1)
            while(!message.endsWith("\\'")) {
              message += "\n" + inputBuffer.readLine()
            }
            Publish(topic, message, client)
        }
      }
    } catch {
      case e: Exception => MalformedCommand(client)
    }
  }
}