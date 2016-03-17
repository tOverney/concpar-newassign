package main.scala

trait MockingFinal {
  implicit val dummy = 0
  
  def wait()(implicit i: Int) = waitDefault()
  
  def synchronized[T](e: =>T) = synchronizedDefault(e)
  
  def notify()(implicit i: Int) = notifyDefault()
  
  def notifyAll()(implicit i: Int) = notifyAllDefault()
  
  def scheduler: Scheduler
  
  // Can be overriden.
  def waitDefault() = scheduler updateThreadState Wait(this, scheduler.threadLocks.tail) // Problem: should remove this from the locks.
  def synchronizedDefault[T](toExecute: =>T) = {
    val prevLocks = scheduler.threadLocks
    scheduler updateThreadState Sync(this, prevLocks) // If this belongs to prevLocks, should just continue.
    toExecute
    scheduler updateThreadState Running(prevLocks)
  }
  def notifyDefault() = scheduler mapOtherStates {
    state => state match {
      case Wait(lockToAquire, locks) if lockToAquire == this => SyncUnique(this, state.locks)
      case e => e
    }
  }
  def notifyAllDefault() = scheduler mapOtherStates {
    state => state match {
      case Wait(lockToAquire, locks) if lockToAquire == this => Sync(this, state.locks)
      case e => e
    }
  }
}
abstract class WaitingState {
  def locks: Seq[AnyRef]
}
trait CanContinueIfAcquiresLock extends WaitingState {
  def lockToAquire: AnyRef
}
case object Start extends WaitingState { def locks: Seq[AnyRef] = Seq.empty }
case object End extends WaitingState { def locks: Seq[AnyRef] = Seq.empty }
case class Wait(lockToAquire: AnyRef, locks: Seq[AnyRef]) extends WaitingState
case class SyncUnique(lockToAquire: AnyRef, locks: Seq[AnyRef]) extends WaitingState with CanContinueIfAcquiresLock
case class Sync(lockToAquire: AnyRef, locks: Seq[AnyRef]) extends WaitingState with CanContinueIfAcquiresLock
case class Running(locks: Seq[AnyRef]) extends WaitingState
case class VariableReadWrite(locks: Seq[AnyRef]) extends WaitingState
