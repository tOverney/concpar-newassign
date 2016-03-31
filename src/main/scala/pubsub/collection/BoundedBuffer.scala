package pubsub.collection

import instrumentation.Schedulable

trait InternalBuffer[T] {
  def update(index: Int, elem: T): Unit
  def apply(index: Int): T
  def delete(index: Int): Unit
  val size: Int
}


abstract class AbstractBoundedBuffer[T](bufferSize: Int) extends Schedulable {
  require(bufferSize > 0)

  def put(element: T): Unit
  def take(): T

  val buffer: InternalBuffer[T] = new InternalBuffer[T] {
    private val buffer: Array[Option[T]] = new Array(bufferSize)
    def update(index: Int, elem: T): Unit = buffer(index) = Some(elem)
    def apply(index: Int): T = buffer(index).get
    def delete(index: Int): Unit = buffer(index) = None
    val size = bufferSize
  }

  def head: Int = _head
  def head_=(e: Int): Unit = _head = e
  def count: Int = _count
  def count_=(e: Int): Unit = _count = e

  private var _head = 0;
  private var _count = 0;
}

class BoundedBuffer[T](size: Int) extends AbstractBoundedBuffer[T](size) {


  def putWrong1(e: T): Unit = {
    while (isFull) {      
    }
    buffer(tail) = e
    count += 1
  }

  def takeWrong1(): T = {
    while (isEmpty) {      
    }
    val toReturn = buffer(head)
    buffer.delete(head)
    head = (head + 1) % size
    count -= 1
    toReturn
  }

  def putWrong2(e: T): Unit = {
    while (isFull) { }
    buffer(tail) = e
    count += 1
  }
  

  /*def takeWrong2(): T = {
    while (isEmpty) {
      Thread.sleep(1)
    }
    lock.synchronized {
      val toReturn = buffer(head)
      buffer.delete(head)
      head = (head + 1) % size
      count -= 1
      toReturn
    }
  }

  def putWrong3(e: T): Unit = {
    lock.synchronized {
      while (isFull) {
        Thread.sleep(1)
      }
      buffer(tail) = e
      count += 1
    }
  }

  def takeWrong3(): T = {
    lock.synchronized {
      while (isEmpty) {
        Thread.sleep(1)
      }
      val toReturn = buffer(head)
      buffer.delete(head)
      head = (head + 1) % size
      count -= 1
      toReturn
    }
  }*/

  // Correct versions.
  override def put(e: T): Unit = {
    this.synchronized {
      while (isFull) {
        this.wait()
      }
      buffer(tail) = e
      count += 1
      this.notifyAll()
    }
  }

  override def take(): T = {
    this.synchronized {
      while (isEmpty) {
        this.wait()
      }
      val toReturn = buffer(head)
      buffer.delete(head)
      head = (head + 1) % size
      count -= 1
      this.notifyAll()
      toReturn
    }
  }
  
  def putCorrect2(e: T): Unit = {
    while (true)  {
      while (isFull) {}
      synchronized {
        if(!isFull) {
          buffer(tail) = e
          count += 1
          return
        }
      }
    }
  }
  
  def takeCorrect2(): T = {
    while (true) {
      while (isEmpty) {}
      synchronized {
        if (!isEmpty) {
          val toReturn = buffer(head)
          buffer.delete(head)
          head = (head + 1) % size
          count -= 1
          return toReturn
        }
      }
    }
    throw new Exception("Not implemented !!")
  }

  def tail: Int = (head + count) % size
  def isFull: Boolean = count == size

  def isEmpty: Boolean = count == 0

}
