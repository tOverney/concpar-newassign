trait MockingFinal {
  implicit val dummy = 0
  
  def wait()(implicit i: Int) = waitDefault()
  
  def synchronized[T](e: =>T) = synchronizedDefault(e)
  
  def notify()(implicit i: Int) = notifyDefault()
  
  def notifyAll()(implicit i: Int) = notifyAllDefault()
  
  // Can be overriden.
  def waitDefault() = super.wait()
  def synchronizedDefault[T](e: =>T) = super.synchronized(e)
  def notifyDefault() = super.notify()
  def notifyAllDefault() = super.notifyAll()
}