package instrumentation

trait Schedulable {
  implicit val dummy = 0
  
  def wait()(implicit i: Int) = waitDefault()
  
  def synchronized[T](e: => T) = synchronizedDefault(e)
  
  def notify()(implicit i: Int) = notifyDefault()
  
  def notifyAll()(implicit i: Int) = notifyAllDefault()
  
  // Can be overriden.
  def waitDefault(): Unit = wait()
  def synchronizedDefault[T](toExecute: =>T): T = synchronized(toExecute)
  def notifyDefault(): Unit = notify()
  def notifyAllDefault(): Unit = notifyAll()
}