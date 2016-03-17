package main.scala

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.JavaConversions._
import java.util.concurrent.Executors
import java.util.Timer
import java.util.TimerTask

@RunWith(classOf[JUnitRunner])
class BoundedBufferSuite extends FunSuite {

/*  test("run concurrent update of the buffer") {
    val queue = new ProducerConsumer[Int](10) with IntegerIndices
    val counter = new AtomicInteger(0)
    val numberProduce = 10 //TODO: correctly close the threads if too many are spawned
    val taskSize = 10
    val threadPool = Executors.newFixedThreadPool(numberProduce + 1)

    val producers = for (i <- 1 to numberProduce) yield Future {
      for (j <- 0 until taskSize) {
        queue.put(i)
      }
    }(threadPool)

    val consumer = Future {
      while(true) {
        val elem = queue.take()
        counter.getAndAdd(elem)
      }
    }(threadPool)

    for (future <- producers) {
      Await.ready(future, 10 seconds)
    }
    Thread.sleep(1000)
    assert(counter.get == (1 to numberProduce).sum * taskSize)
  }*/   

  import TestHelper._
  /*test("Should work with one producer, one consumer and a buffer of size 1") {    
    testManySchedules((1 to scheduleLength).flatMap(_ => List(1, 2)).toList, sched => {
      val prodCons = new SchedProducerConsumer[Int](1, sched)
      val ops = List(() => prodCons.putWrong1(1), () => prodCons.takeWrong1())
      sched.runInParallel(ops)
    })       
  }*/
  /*
  test("Should work with 2 producers, one consumer and a buffer of size 1") {
    testManySchedules((1 to scheduleLength).flatMap(_ => List(1, 2, 3)).toList, sched => {
      val prodCons = new SchedProducerConsumer[Int](1, sched)
      val ops = List(() => prodCons.putWrong1(1), () => prodCons.putWrong1(2), () => prodCons.takeWrong1())
      sched.runInParallel(ops)
    })           
  }
  */
  test("Testing a case of deadlock") {    
    testManySchedules((1 to scheduleLength).flatMap(_ => List(1, 2)).toList, sched => {
      val prodCons = new SchedProducerConsumer[Int](1, sched)
      val ops = List(() => prodCons.putWrong2(1), () => prodCons.take())
      sched.runInParallel(ops)
    })       
  }
  /*
  test("Should work with 3 producers, 2 consumer and a buffer of size 1") {
    testManySchedules((1 to scheduleLength).flatMap(_ => List(1, 2, 3, 4, 5)).toList, sched => {
      val prodCons = new SchedProducerConsumer[Int](1, sched)
      val ops = List(() => prodCons.putWrong1(1), () => prodCons.putWrong1(2), () => prodCons.putWrong1(3), 
          () => prodCons.takeWrong1(), () => prodCons.takeWrong1())
      /*val ops = List(() => prodCons.put(1), () => prodCons.put(2), () => prodCons.put(3),
        () => prodCons.take(), () => prodCons.take())*/
      sched.runInParallel(ops)
    })           
  }  */
}

object TestHelper {
  val noOfSchedules = 10000  
  val scheduleLength = 20 // maximum number of read/writes possible in one thread
  val testTimeout = 60 // the total time out for a test in seconds
  val schedTimeout = 10 // the total time out for execution of a schedule in secs
  
  def testManySchedules(firstSched: List[Int], fun: Scheduler => Unit) = {
    // create a timer thread for timing out    
    val testTimer = new Timer()
    testTimer.schedule(new TimerTask {
      override def run() {                
        println("Test took too long to complete!")
        Runtime.getRuntime().halt(0) //exit the JVM and all running threads (no other way to kill other threads)
      }
    }, testTimeout * 1000)
    firstSched.permutations.take(noOfSchedules).foreach { schedule =>
      val schedr = new Scheduler(schedule)
      val schedTimer = new Timer()
      schedTimer.schedule(new TimerTask {
        override def run() {
          println("The schedule took too long to complete. A possible deadlock!")
          println(schedr.getOperationLog().mkString("\n"))
          Runtime.getRuntime().halt(0) //exit the JVM and all running threads (no other way to kill other threads)
        }
      }, schedTimeout * 1000)    
      //println("Exploring Sched: "+schedule)      
      //println("Testing configuration " + schedule)
      fun(schedr)   
      schedTimer.cancel()
    }
    testTimer.cancel()
  }  
}
