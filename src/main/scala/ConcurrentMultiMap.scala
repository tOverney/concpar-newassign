package boundedbuffer

import scala.collection.mutable
import java.util.concurrent.locks.ReentrantReadWriteLock

class ConcurrentMultiMap[K,V] {

  val lock = new ReentrantReadWriteLock()
  val map = mutable.HashMap[K, Set[V]]()

  def add(key: K, value: V): Unit = {
    try {
      lock.writeLock().lock()
      map.get(key) match {
        case Some(set) =>
          if (! set.contains(value)) {
            map += ((key, set + value))
          }
        case None =>
          map += ((key, Set(value)))
      }
    } finally {
      lock.writeLock().unlock()
    }
  }

  def get(key: K): Option[Set[V]] = {
    try {
      lock.readLock().lock()
      val v = map.get(key)
      v
    } finally {
      lock.readLock().unlock()
    }
  }

  def remove(key: K, value: V): Unit = {
    try {
      lock.writeLock().lock()
      map.get(key) match {
        case Some(set) =>
          if (set.contains(value)) {
            map += ((key, set - value))
          }
        case None =>
      }
    } finally {
      lock.writeLock().unlock()
    }
  }

}
