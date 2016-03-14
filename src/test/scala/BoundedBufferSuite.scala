package boundedbuffer

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

@RunWith(classOf[JUnitRunner])
class BoundedBufferSuite extends FunSuite {

  test("run concurrent update of the buffer") {
    val queue = new BoundedBuffer[Int](10)
    val counter = new AtomicInteger(0)
    val numberProduce = 10
    val taskSize = 10

    val consumer = Future {
      while(true) {
        val elem = queue.take()
        counter.getAndAdd(elem)
      }
    }

    val producers = for (i <- 1 to numberProduce) yield Future {
      for (j <- 0 until taskSize) {
        queue.put(i)
      }
    }

    for (future <- producers) {
      Await.ready(future, 100 seconds)
    }
    Thread.sleep(1000)
    assert(counter.get == (1 to numberProduce).sum * taskSize)
  }

}
