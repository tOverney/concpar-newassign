# PubSub server

The goal of this assignment is to create a concurrent bounded buffer for use in a PubSub server.

A concurrent bounded buffer is a simple data structure that supports the following two operations:
- `put` a value in the buffer,
- `take` a value from the buffer and remove it.

The concurrent bounded buffer you will implement will have FIFO (First-In, First-Out) semantics, meaning that `take` should always return the oldest element `put` in the data structure. 

The buffer is *bounded*, which means that it can only contain a finite number of elements. This maximum number of elements is passed as an argument to the constructor of `BoundedBuffer`.

Since this buffer is of finite size, some operations may not be possible depending on the state of the buffer.
If the buffer contains no element, then `take` is not currently possible. Likewise, `put` is not possible when the buffer is full.
When an operation is not possible, the thread should wait until the state of the `BoundedBuffer` permits the operation to be carried on.

This exercise is composed of two parts:
- In the first part, we will present you different implementations of the `put` and `take` methods, all presenting some concurrency issues. Your goal will be to understand the issues with the solutions.
- In the second part, you will have to come up with a correct implementation of the `put` and `take` methods.

## First part

### Non-concurrent Bounded-Buffer

The initial code you are given is an non-concurrent implementation of the bounded buffer datatype.

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

Instead of waiting, impossible operations throw an exception. In program consisting of a single thread, it would make no sense to wait until the buffer state is modified by other threads, since there aren't any.

What can go wrong with this solution in a concurrent setting? Try to come up with a schedule in which something bad happens! It could either be that:
- An operation returns an incorrect value.
- An operation throws an exception when it should not.

A schedule is a sequence of operations carried out by the different threads. The operations of a thread can be interleaved with operations from multiple other threads. Also remember that some operations, even through they look *atomic*, are decomposed into multiple smaller operations.

The test suite of the project should help you find problematic schedules. Try to understand what went wrong with them.

[comment]: # (Should we talk about the memory model here?)

### Busy waiting

In the previous code snippet, the operations threw an exception when the operation to be performed was not possible due to the buffer being either already empty or full.
Instead of throwing an exception, we would like the thread performing the operation to wait until the operation becomes possible and then perform it. 

The code below performs this using a while loop that guards the execution of an operation.

```scala
class BoundedBuffer[T](size: Int) extends AbstractBoundedBuffer[T](size) {

  override def put(e: T): Unit = {
    while (isFull) {}
    buffer(tail) = e
    count += 1
  }

  override def take(): T = {
    while (isEmpty) {}
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

What is the problem with this way of waiting?

Apart with those potential problems, is this code behaving correctly in a concurrent setting?

## Second part

In the last section, we looked at different implementations of the bounded buffer and tried to identify problems with them. In this section, you will implement a correct concurrent bounded buffer using synchronisation primitives.



