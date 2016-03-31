package pubsub

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

import instrumentation._
import instrumentation.Stats._

import pubsub.collection._

@RunWith(classOf[JUnitRunner])
class BoundedBufferSuite extends FunSuite {

  import TestHelper._
  test("Should work with one producer, one consumer and a buffer of size 1") {
    testManySchedules(2, sched => {
      val prodCons = new SchedulableBoundedBuffer[Int](1, sched)
      List(() => prodCons.put(1), () => prodCons.take())
    })    
  }

  test("Should work with 2 producers, one consumer and a buffer of size 1") {
    testManySchedules(3, sched => {
      val prodCons = new SchedulableBoundedBuffer[Int](1, sched)
      List(() => prodCons.put(1), () => prodCons.put(2), () => prodCons.take())
    })    
  }

  test("Should work with 3 producers, 2 consumer and a buffer of size 1") {
    testManySchedules(5, sched => {
      val prodCons = new SchedulableBoundedBuffer[Int](1, sched)
      List(() => prodCons.put(1), () => prodCons.put(2), () => prodCons.put(3),
        () => prodCons.take(), () => prodCons.take())      
    })    
  }

  test("Should work with 2 producers and 2 consumers and a buffer of size 1") {
    testManySchedules(4, sched => {
      val prodCons = new SchedulableBoundedBuffer[Int](1, sched)
      List(() => prodCons.put(1), () => prodCons.put(2), () => prodCons.take(), () => prodCons.take())
    })    
  }
}