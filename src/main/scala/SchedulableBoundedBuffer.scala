package main.scala

import java.util.concurrent._;
import scala.concurrent.duration._
import scala.collection.mutable._

/**
 * A class that maintains schedule and a set of thread ids.
 * The schedules are advanced after an operation of a SchedulableBuffer is performed.
 * Note: the real schedule that is executed may deviate from the input schedule
 * due to the adjustments that had to be made for locks
 */
class Scheduler(sched: List[Int]) {  
  val iterationBeforeGoingOutOfTurn = 1000000  
  
  private var schedule = sched
  private var realToFakeThreadId = Map[Long, Int]()
  private val opLog = ListBuffer[String]() // a mutable list (used for efficient concat)  
    
  /**
   * Runs a set of operations in parallel as per the schedule.
   * Each operation may consist of many primitive operations like reads or writes
   * to shared data structure each of which should be executed using the function `exec`.
   */
  def runInParallel[T](ops: List[() => Any]) {
    // create threads    
    val threads = ops.zipWithIndex.map {
      case (op, i) =>
        new Thread(new Runnable() {
          def run() {
            val fakeId = i + 1
            setThreadId(fakeId)
            op()
            removeFromSchedule(fakeId)
          }
        })
    }
    // start all threads
    threads.foreach(_.start())
    // wait for all threads to complete
    threads.foreach(_.join())
  }

  /**
   * Executes a read or write operation to a global data structure as per the given schedule
   * @param msg a message corresponding to the operation that will be logged 
   */
  def exec[T](primop: => T)(msg: String): T = {
    waitForTurn
    opLog += threadId + ":" + msg
    val res = primop
    advanceSchedule
    res
  }   

  private def setThreadId(fakeId: Int) = synchronized {
    realToFakeThreadId += Thread.currentThread.getId -> fakeId
  }
  
  private def threadId = 
    realToFakeThreadId(Thread.currentThread().getId())

  private def isTurn(tid: Int) = synchronized {
    (!schedule.isEmpty && schedule.head != tid)
  } 

  /**
   * This will be called before a schedulable operation begins.
   * This should not use synchronized
   */
  var enteredInTurn = false // has anybody entered in this turn ?
  private def waitForTurn = {
    var iterCount = 0
    val tid = threadId
    while ((enteredInTurn == true || iterCount <= iterationBeforeGoingOutOfTurn) &&
      isTurn(tid)) {
      iterCount += 1
    }
    enteredInTurn = true    
    //println("Real Schedule: " + realSchedule)
    //println(s"Turn: ${schedule.headOption.getOrElse("Any")} executing: $tid")
  }

  /**
   * This will be called when a schedulable operation is about to complete.
   * Note: it doesn't use synchronized as only thread will execute a schedulable operation.
   */
  private def advanceSchedule = {
    enteredInTurn = false
    if (!schedule.isEmpty)
      schedule = schedule.tail
  }

  /**
   * To be invoked when a thread is about to complete
   */
  private def removeFromSchedule(fakeid: Int) = synchronized {
    schedule = schedule.filterNot(_ == fakeid)
  }
  
  def getOperationLog() = opLog
}

class SchedulableBoundedBuffer[T](val size: Int, sched: Scheduler) extends InternalBuffer[T] {
  private val buffer = new Array[Option[T]](size)

  def update(index: Int, elem: T) {
    sched.exec {
      buffer(index) = Some(elem)
    }(s"Write buffer($index) = $elem")
  }

  def apply(index: Int): T = sched.exec {
    buffer(index).get
  }(s"Read buffer($index)")

  def delete(index: Int) {
    sched.exec { buffer(index) = None }(s"Delete buffer($index)")
  }
}

class SchedProducerConsumer[T](size: Int, sched: Scheduler) extends ProducerConsumer[T](size) {

  override def createBuffer(_size: Int) = new SchedulableBoundedBuffer(size, sched)
  var h: Int = 0
  var c: Int = 0

  def head_=(i: Int) = sched.exec { h = i }(s"Write head=$i")
  def head: Int = sched.exec { h }(s"Read head")
  def count_=(i: Int) = sched.exec { c = i }(s"Write count=$i")
  def count: Int = sched.exec { c }(s"Read count")
}

