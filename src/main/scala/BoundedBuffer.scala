package boundedbuffer

import scala.reflect.ClassTag

class BoundedBuffer[E](size: Int, default: E)(implicit m: ClassTag[E]) {

  val buffer: Array[E] = (1 to size).map(_ => default).toArray
  var head = 0
  var count = 0

  val lock = new AnyRef

  def put(e: E): Unit = {
    lock.synchronized {
      while (isFull) {
        lock.wait()
      }
      buffer((head + count) % size) = e
      count += 1
      lock.notifyAll()
    }
  }

  def take(): E = {
    lock.synchronized {
      while (isEmpty) {
        lock.wait()
      }
      val toReturn = buffer(head)
      buffer(head) = default
      head = (head + 1) % size
      count -= 1
      lock.notifyAll()
      toReturn
    }
  }

  def isFull: Boolean = (count == size)

  def isEmpty: Boolean = (count == 0)

}
