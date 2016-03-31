# PubSub server

The goal of this assignment is to create a concurrent bounded buffer for use in a PubSub server.

A concurrent bounded buffer is a simple data structure that supports the following two operations:
- `put` a value in the buffer,
- `take` a value from the buffer.

The concurrent bounded buffer you will implement will have FIFO (First-In, First-Out) semantics, meaning that `take` should always return the oldest element `put` in the data structure. 

The buffer is *bounded*, which means that it can only contain a finite number of elements. This maximum number of elements is passed as an argument to the constructor of `BoundedBuffer`.

Since this buffer is of finite size, some operations may not be possible depending on the state of the buffer.
If the buffer contains no element, then `take` is not currently possible. Likewise, `put` is not possible when the buffer is full.
When an operation is not possible, the thread should wait until the state of the `BoundedBuffer` permits the operation to be carried on.

This exercise is composed of two parts:
- In the first part, we will present you different implementations of the `put` and `take` methods, all presenting some concurrency issues. Your goal will be to understand the issues with the solutions.
- In the second part, you will have to come up with a correct implementation of the `put` and `take` methods.

## First part

The initial code you are given is a correct implementation of the bounded buffer datatype, assuming a non-concurrent setting.

```scala
class BoundedBuffer[T](size: Int) extends AbstractBoundedBuffer[T](size) {

  override def put(e: T): Unit = {
    if (isFull) {
      throw new Exception("Buffer is full!")
    }
    buffer(tail) = e
    count += 1
  }

  override def take(): T = {
    if (isEmpty) {
      throw new Exception("Buffer is empty!")
    }
    val ret = buffer(head)
    buffer.delete(head)
    head = (head + 1) % size
    count -= 1
    ret
  }

  def tail: Int = (head + count) % size
  def isFull: Boolean = count == size
  def isEmpty: Boolean = count == 0
}
```

What can go wrong with this solution?

## Second part

