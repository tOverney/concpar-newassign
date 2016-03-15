package main.scala

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.InputStream

class CommandReader(inStream: InputStream, client: Client) {
  val inputBuffer = new BufferedReader(new InputStreamReader(inStream))

  def fetchCommand(): Command = {
    val line = inputBuffer.readLine()

    if (line == null || line.startsWith("leave")) {
      EndOfClient(client)
    }
    else {
      val (command :: payload) = line.split(" \\'").toList
      val parts = (command.split(" ") ++ payload).toList

      parts match {
        case "subscribe" :: topic :: Nil   => Subscribe(topic, client)
        case "unsubscribe" :: topic :: Nil => Unsubscribe(topic, client)

        case "publish" :: topic :: msg :: Nil => 
          var message = s"'$msg"
          while(!message.endsWith("\'")) {
            message += "\n" + inputBuffer.readLine()
          }
          Publish(topic, message, client)

        case _ => MalformedCommand(client)
      }
    }
  }
}
