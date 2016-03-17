package main.scala

trait InternalBuffer[T] {
  def update(index: Int, elem: T): Unit
  def apply(index: Int): T
  def delete(index: Int): Unit
  val size: Int
}

trait IntegerIndices { self: ProducerConsumer[_] =>
  var head: Int = 0
  var count: Int = 0
}

abstract class ProducerConsumer[T](size: Int) extends Schedulable { self =>
  require(size > 0)

  def createBuffer(_size: Int) = new InternalBuffer[T] {
    private val buffer: Array[Option[T]] = new Array(_size)
    def update(index: Int, elem: T): Unit = buffer(index) = Some(elem)
    def apply(index: Int): T = buffer(index).get
    def delete(index: Int): Unit = buffer(index) = None
    val size = _size
  }

  val buffer: InternalBuffer[T] = createBuffer(size)

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

  def tail: Int = (head + count) % size
  def isFull: Boolean = count == size

  def isEmpty: Boolean = count == 0

}
