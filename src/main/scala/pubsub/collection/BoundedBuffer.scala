package pubsub.collection

import instrumentation.Schedulable

trait InternalBuffer[T] {
  def update(index: Int, elem: T): Unit
  def apply(index: Int): T
  def delete(index: Int): Unit
  val size: Int
}

trait ConcreteInternals[T] { self: BoundedBuffer[T] =>
  override var head: Int = 0
  override var count: Int = 0

  override val buffer: InternalBuffer[T] = new InternalBuffer[T] {
    private val buffer: Array[Option[T]] = new Array(self.bufferSize)
    def update(index: Int, elem: T): Unit = buffer(index) = Some(elem)
    def apply(index: Int): T = buffer(index).get
    def delete(index: Int): Unit = buffer(index) = None
    val size = self.bufferSize
  }
}

abstract class BoundedBuffer[T](size: Int) extends Schedulable {
  require(size > 0)

  val bufferSize = size
  val buffer: InternalBuffer[T]

  // Emulate "var head, count: Int = 0"
  def head: Int
  def head_=(e: Int): Unit
  def count: Int
  def count_=(e: Int): Unit

  /*def init() {
    head = 0
    count = 0
  }*/

  val lock = new AnyRef

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
  def put(e: T): Unit = {
    this.synchronized {
      while (isFull) {
        this.wait()
      }
      buffer(tail) = e
      count += 1
      this.notifyAll()
    }
  }

  def take(): T = {
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