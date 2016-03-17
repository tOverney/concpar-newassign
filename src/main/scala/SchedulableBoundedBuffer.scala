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
  val maxOps = 10000 // a limit on the maximum number of operations the code is allowed to perform
    
  private var schedule = sched
  private var numThreads = 0
  private val realToFakeThreadId = Map[Long, Int]()
  private val opLog = ListBuffer[String]() // a mutable list (used for efficient concat)   
  private val threadStates = Map[Int, WaitingState]()
    
  // Helpers
  def runInParallel(op1: =>Any): Unit = runInParallel(List(() => op1))
  def runInParallel(op1: =>Any, op2: =>Any): Unit = runInParallel(List(() => op1, () => op2))
  def runInParallel(op1: =>Any, op2: =>Any, op3: =>Any): Unit = runInParallel(List(() => op1, () => op2, () => op3))
  def runInParallel(op1: =>Any, op2: =>Any, op3: =>Any, op4: =>Any): Unit = runInParallel(List(() => op1, () => op2, () => op3, () => op4))
  
  /**
   * Runs a set of operations in parallel as per the schedule.
   * Each operation may consist of many primitive operations like reads or writes
   * to shared data structure each of which should be executed using the function `exec`.
   */
  def runInParallel(ops: List[() => Any]) {
    numThreads = ops.length
    // create threads    
    val threads = ops.zipWithIndex.map {
      case (op, i) =>
        new Thread(new Runnable() {
          def run() {
            val fakeId = i + 1
            setThreadId(fakeId)
            try {
              updateThreadState(Start)
              op()
              updateThreadState(End)
            } catch {
              case e: Exception =>
                println(s"Thread $fakeId threw Exception on the following schedule:")
                println(opLog.mkString("\n"))
                println(s"$fakeId: ${e.toString}")
                Runtime.getRuntime().halt(0) //exit the JVM and all running threads (no other way to kill other threads)                
            }
            removeFromSchedule(fakeId)
          }
        })
    }
    // start all threads
    threads.foreach(_.start())
    // Now the scheduling operation.
    
    
    // wait for all threads to complete
    threads.foreach(_.join())
  }
  
  // Updates the state of the current thread
  def updateThreadState(state: WaitingState): Unit = {
    val tid = threadId
    synchronized {
      threadStates(tid) = state
    }
    state match {
      case Sync(lockToAquire, locks) =>
        if(locks.indexOf(lockToAquire) < 0) waitForTurn else {
          updateThreadState(Running(lockToAquire +: locks)) // Keep locks as a FIFO;
        }
      case Start => waitStart()
      case End => removeFromSchedule(tid)
      case Running(_) =>
      case _ => waitForTurn // Wait, SyncUnique, VariableReadWrite
    }
  }
  
  
  def waitStart() {
    waitingForDecision(threadId) = None
    while(threadStates.size < numThreads) {
      Thread.sleep(1)
    }
  }
  
  def threadLocks = {
    synchronized {
      threadStates(threadId).locks
    }
  }
  
  def mapOtherStates(f: WaitingState => WaitingState) = {
    val exception = threadId
    synchronized {
      for(k <- threadStates.keys if k != exception) {
        threadStates(k) = f(threadStates(k))
      }
    }
  }

  /**
   * Executes a read or write operation to a global data structure as per the given schedule
   * @param msg a message corresponding to the operation that will be logged 
   */
  def exec[T](primop: => T)(msg: =>String): T = {
    updateThreadState(VariableReadWrite(threadLocks))
    opLog += (" " * ((threadId - 1)*2)) + threadId + ":" + msg
    if(opLog.size > maxOps)
      throw new Exception(s"Total number of reads/writes performed by threads exceed $maxOps. A possible deadlock!")
    primop
  }

  private def setThreadId(fakeId: Int) = synchronized {
    realToFakeThreadId(Thread.currentThread.getId) = fakeId
  }
  
  private def threadId = 
    realToFakeThreadId(Thread.currentThread().getId())

  private def isTurn(tid: Int) = synchronized {
    (!schedule.isEmpty && schedule.head != tid)
  }
  
  /** returns true if the thread can continue to execute, and false otherwise */
  def decide(force: Boolean = false): Boolean = {
    val tid = threadId
    canContinue match {
      case Some((i, state)) if i == tid =>
        waitingForDecision(i) = None // Make sure canContinue will not be recomputed.
        canContinue = None
        state match {
          case Sync(lockToAquire, locks) => updateThreadState(Running(lockToAquire +: locks))
          case SyncUnique(lockToAquire, locks) =>
            mapOtherStates {
              state => state match {
                case SyncUnique(lockToAquire2, locks2) if lockToAquire2 == lockToAquire => Wait(lockToAquire2, locks2)
                case e => e
              }
            }
            updateThreadState(Running(lockToAquire +: locks))
          case VariableReadWrite(locks) => updateThreadState(Running(locks))
        }
        true
      case Some((i, state)) => false
      case None =>
      synchronized {
          // If all remaining threads are waiting for a decision.
        if (waitingForDecision.values.forall(_.nonEmpty)) {
          // If this thread is the last one to 
          if (force || waitingForDecision(tid) == numToEnter - 1) { // The last thread who enters the decision loop takes the decision.
            if (threadStates.values.forall { case e: Wait => true case _ => false} && threadStates.size >= 1) {
              val waiting = threadStates.keys.map(_.toString).mkString(", ")
              val s = threadStates.size > 1
              val are = if(threadStates.size > 1) "are" else "is"
              throw new Exception(s"Deadlock: Thread$s $waiting $are waiting but all others have ended and cannot notify them.")
            } else {
              // Threads can be in Wait, Sync, SyncUnique, and VariableReadWrite mode.
              // Let's determine which ones can continue.
              val notFree = threadStates.collect{ case (id, state) => state.locks }.flatten.toSet
              val whoHasLock = threadStates.toSeq.flatMap{ case (id, state) => state.locks.map(lock => (lock, id))}.toMap
              val threadsNotBlocked = threadStates.toSeq.filter{
                case (id, v: VariableReadWrite) => true
                case (id, v: CanContinueIfAcquiresLock) if !notFree(v.lockToAquire) || v.locks.indexOf(v.lockToAquire) >= 0 => true
                case _ => false
              }
              if (threadsNotBlocked.isEmpty) {
                val waiting = threadStates.keys.map(_.toString).mkString(", ")
                val s = threadStates.size > 1
                val are = if(threadStates.size > 1) "are" else "is"
                val reason = threadStates.collect{
                  case (id, state: CanContinueIfAcquiresLock) if !notFree(state.lockToAquire) =>
                  s"Thread $id is waiting on lock ${state.lockToAquire} held by thread ${whoHasLock(state.lockToAquire)}" }.mkString("\n")
                throw new Exception(s"Deadlock: Thread$s $waiting are interlocked. Indeed:\n$reason")
              } else {
                val next = schedule.indexWhere(t => threadsNotBlocked.exists{ case (id, state) => id == t})
                if (next != -1) {
                  schedule = schedule.drop(next)
                  val chosenOne = schedule.head
                  schedule = schedule.drop(1)
                  canContinue = Some((chosenOne, threadStates(chosenOne)))
                  if(chosenOne == tid) decide() else false
                } else {
                  throw new Exception("The schedule is " + schedule.mkString(",") + " but only threads $threadsNotBlocked are not blocked")
                }
              }
            }
          } else false
        } else false
      }
    }
  }

  /**
   * This will be called before a schedulable operation begins.
   * This should not use synchronized
   */
  var numToEnter = 0
  var waitingForDecision = Map[Int, Option[Int]]() // Mapping from thread ids to a number indicating who is going to make the choice.
  var canContinue: Option[(Int, WaitingState)] = None // The result of the decision thread Id of the thread authorized to continue.
  private def waitForTurn = {
    val tid = threadId
    synchronized { // The last thread who
      numToEnter += 1
      waitingForDecision(tid) = Some(numToEnter)
    }
    var iterCount = 0
    while (iterCount <= iterationBeforeGoingOutOfTurn && !decide()) {
      iterCount += 1
    }
    synchronized {
      waitingForDecision(tid) = None  
      numToEnter -= 1
    }
  }

  /**
   * To be invoked when a thread is about to complete
   */
  private def removeFromSchedule(fakeid: Int) = synchronized {
    schedule = schedule.filterNot(_ == fakeid)
    waitingForDecision -= fakeid
    threadStates -= fakeid
    decide(force = true)
  }
  
  def getOperationLog() = opLog
}

class SchedulableBoundedBuffer[T](val size: Int, scheduler: Scheduler) extends InternalBuffer[T] {
  private val buffer = new Array[Option[T]](size)

  def update(index: Int, elem: T) {
    scheduler.exec {
      buffer(index) = Some(elem)
    }(s"Write buffer($index) = $elem")
  }

  def apply(index: Int): T = scheduler.exec {
    buffer(index).get
  }(s"Read buffer($index)")

  def delete(index: Int) {
    scheduler.exec { buffer(index) = None }(s"Delete buffer($index)")
  }
}

class SchedProducerConsumer[T](size: Int, val scheduler: Scheduler) extends ProducerConsumer[T](size) with MockingFinal {

  override def createBuffer(_size: Int) = new SchedulableBoundedBuffer(size, scheduler)
  var h: Int = 0
  var c: Int = 0

  def head_=(i: Int) = scheduler.exec { h = i }(s"Write head<-$i")
  def head: Int = scheduler.exec { h }(s"Read head->$h")
  def count_=(i: Int) = scheduler.exec { c = i }(s"Write count<-$i")
  def count: Int = scheduler.exec { c }(s"Read count->$c")
}

