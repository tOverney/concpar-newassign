package main.scala

class BoundedBuffer[E](size: Int) {
  require(size > 0)

  val buffer: Array[Option[E]] = new Array(size)
  var head = 0
  var count = 0

  val lock = new AnyRef

  def put(e: E): Unit = {
    this.synchronized {
      while (isFull) {
        this.wait()
      }
      buffer(tail) = Some(e)
      count += 1
      this.notifyAll()
    }
  }

  def take(): E = {
    this.synchronized {
      while (isEmpty) {
        this.wait()
      }
      val toReturn = buffer(head).get
      buffer(head) = None
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
