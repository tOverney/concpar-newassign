package main.scala

import scala.reflect.ClassTag

class BoundedBuffer[E](size: Int)(implicit m: ClassTag[E]) {

  val buffer: Array[Option[E]] = new Array(size)
  var head = 0
  var count = 0

  val lock = new AnyRef

  def put(e: E): Unit = {
    lock.synchronized {
      while (isFull) {
        lock.wait()
      }
      buffer(tail) = Some(e)
      count += 1
      lock.notifyAll()
    }
  }

  def take(): E = {
    lock.synchronized {
      while (isEmpty) {
        lock.wait()
      }
      val toReturn = buffer(head).get
      buffer(head) = None
      head = (head + 1) % size
      count -= 1
      lock.notifyAll()
      toReturn
    }
  }

  def tail: Int = (head + count) % size
  def isFull: Boolean = count == size

  def isEmpty: Boolean = count == 0

}
