package main.scala

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.JavaConversions._
import java.util.concurrent.Executors

@RunWith(classOf[JUnitRunner])
class BoundedBufferSuite extends FunSuite {

  test("run concurrent update of the buffer") {
    val queue = new BoundedBuffer[Int](10) with IntegerIndices
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
  }
  
  trait DelayedThreadExecutionBuffer[T] extends BoundedBuffer[T] {
    override def createBuffer(_size: Int) = new InternalBuffer[T] {
      private val buffer: Array[Option[T]] = new Array(_size)
      def update(index: Int, elem: T): Unit = buffer(index) = Some(elem)
      def apply(index: Int): T = buffer(index).get
      def delete(index: Int): Unit = buffer(index) = None
      val size = _size
    }
    def head_=(i: Int) = ???
    def head: Int = ???
    def count_=(i: Int) = ???
    def count: Int = ???
  }

  
  test("Works with different interleavings") {
    val queue = 0
  
  }
}
