package instrumentation

trait Schedulable {
  implicit val dummy = 0
  
  def wait()(implicit i: Int) = waitDefault()
  
  def synchronized[T](e: => T) = synchronizedDefault(e)
  
  def notify()(implicit i: Int) = notifyDefault()
  
  def notifyAll()(implicit i: Int) = notifyAllDefault()
  
  private val lock = new AnyRef

  // Can be overriden.
  def waitDefault(): Unit = lock.wait()
  def synchronizedDefault[T](toExecute: =>T): T = lock.synchronized(toExecute)
  def notifyDefault(): Unit = lock.notify()
  def notifyAllDefault(): Unit = lock.notifyAll()
}