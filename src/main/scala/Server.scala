package main.scala

import java.net.ServerSocket
import java.net.Socket
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL

import java.util.concurrent.Executors
import scala.concurrent.JavaConversions._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object Server extends App {
  val port = 7676
  val maxWorkers = 12
  val bufferSize = 20
  val socket = new ServerSocket(port)
  try {
    val whatismyip = new URL("http://checkip.amazonaws.com")
    val in = new BufferedReader(new InputStreamReader(whatismyip.openStream()));
    val serverIP = in.readLine()
    println(s"Connect to $serverIP (or `localhost`), port $port with `telnet` to join this server")
  } catch  {
    case e: Exception =>
      println("There is a problem with your internet connection, you can only access it via localhost")
  }

  val buffer = new BoundedBuffer[Command](20) with IntegerIndices
  val commandHandlers = for{
    i <- 0 until maxWorkers
  } yield {
    Future {
      new CommandHandler(buffer).handle()
    }
  }
  val threadPool = Executors.newFixedThreadPool(maxWorkers)

  var clientId = 0
  while(true) {
    val client = socket.accept();
    val cid = clientId
    clientId += 1
    Future{
      new TCPReader(clientId, client, buffer).read()
    }(threadPool)
  }
}
