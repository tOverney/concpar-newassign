package instrumentation

import scala.util.Random
import scala.collection.mutable.{Map => MutableMap}

import Stats._

object TestHelper {
  val noOfSchedules = 10000 // set this to 100k during deployment
  val readWritesPerThread = 10 // maximum number of read/writes possible in one thread
  val contextSwitchBound = 10
  val testTimeout = 120 // the total time out for a test in seconds
  val schedTimeout = 15 // the total time out for execution of a schedule in secs

  // Helpers
  /*def testManySchedules(op1: => Any): Unit = testManySchedules(List(() => op1))
  def testManySchedules(op1: => Any, op2: => Any): Unit = testManySchedules(List(() => op1, () => op2))
  def testManySchedules(op1: => Any, op2: => Any, op3: => Any): Unit = testManySchedules(List(() => op1, () => op2, () => op3))
  def testManySchedules(op1: => Any, op2: => Any, op3: => Any, op4: => Any): Unit = testManySchedules(List(() => op1, () => op2, () => op3, () => op4))*/

  def testManySchedules(numThreads: Int, ops: Scheduler => List[() => Any]) = {
    var timeout = testTimeout * 1000L
    val threadIds = (1 to numThreads)
    //(1 to scheduleLength).flatMap(_ => threadIds).toList.permutations.take(noOfSchedules).foreach {
    val schedules = (new ScheduleGenerator(numThreads)).schedules()
    var schedsExplored = 0
    schedules.takeWhile(_ => schedsExplored <= noOfSchedules && timeout > 0).foreach{
      //case _ if timeout <= 0 => // break
      case schedule =>
        schedsExplored += 1 
        val schedr = new Scheduler(schedule)
        //println("Exploring Sched: "+schedule)      
        val threadOps = ops(schedr)
        if (threadOps.size != numThreads)
          throw new IllegalStateException(s"Number of threads: $numThreads, do not match operations of threads: $threadOps")
        timed { schedr.runInParallel(schedTimeout * 1000, threadOps) } { t => timeout -= t } match {
          case Timeout(msg) =>
            println("The schedule took too long to complete. A possible deadlock!")
            println(msg)
            throw new java.lang.AssertionError("assertion failed")
          case Except(msg) =>
            println(msg)
            throw new java.lang.AssertionError("assertion failed")
          case RetVal(threadRes) =>
            threadRes
        }
    }
    if (timeout <= 0) {
      println("Test took too long to complete! Cannot check all schedules as your code is too slow!")
      assert(false)
    }
  }
  
  /**
   * A schedule generator that is based on the context bound
   */
  class ScheduleGenerator(numThreads: Int) extends {
    val scheduleLength = readWritesPerThread * numThreads
    val rands = (1 to scheduleLength).map(i => new Random()) // random numbers for choosing a thread at each position 
    
    def schedules(): Stream[List[Int]] = {
      var contextSwitches = 0
      var contexts = List[Int]() // a stack of thread ids in the order of context-switches      
      val remainingOps = MutableMap[Int, Int]()
      remainingOps ++= (1 to numThreads).map(i => (i, readWritesPerThread)) // num ops remaining in each thread      
      val liveThreads = (1 to numThreads).toSeq.toBuffer

      /**
       * Updates remainingOps and liveThreads once a thread is chosen for a position in the schedule 
       */
      def updateState(tid: Int) {
        val remOps = remainingOps(tid)
        if (remOps == 0) {
          liveThreads -= tid
        } else {
          remainingOps += (tid -> (remOps - 1))
        }
      }
      val schedule = rands.foldLeft(List[Int]()){
        case (acc, r) if contextSwitches < contextSwitchBound =>          
          val tid = liveThreads(r.nextInt(liveThreads.size))
          contexts match {
            case prev :: tail if prev != tid => // we have a new context switch here 
              contexts  +:= tid 
              contextSwitches += 1
            case prev :: tail =>
            case _ => // init case
              contexts +:= tid
          }
          updateState(tid)            
          acc :+ tid
        case (acc, _) => // here context-bound has been reached so complete the schedule without any more context switches
          if(!contexts.isEmpty) {
            contexts = contexts.dropWhile(remainingOps(_) == 0) 
          }
          val tid = contexts match {
            case top :: tail => top              
            case _ => liveThreads(0)  // here, there has to be threads that have not even started              
          }
          updateState(tid)
          acc :+ tid
      }
      schedule #:: schedules()
    }
  }
}