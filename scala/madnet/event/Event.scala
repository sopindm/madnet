package madnet.event

class Set[T] extends scala.collection.mutable.Set[T] {
  private [this] var _set = scala.collection.immutable.Set[T]()
  def snapshot = _set

  override def +=(e: T) = { _set += e; this }
  override def -=(e: T) = { _set -= e; this }
  override def contains(e: T) = _set.contains(e)
  override def iterator = _set.iterator
}

trait Closeable extends java.io.Closeable {
  override def close {}
}

trait IEventHandler extends Closeable {
  private[this] val _emitters = new Set[IEvent]
  def emitters = _emitters.snapshot

  def call(emitter: IEvent, source: Any) {}

  override def close { _emitters.foreach(_ -= this); super.close() }

  private[event] def link(e: IEvent) { _emitters += e }
  private[event] def unlink(e: IEvent) { _emitters -= e }
}

class EventHandler extends IEventHandler

trait ISet[T] {
  def +=(e: T): ISet[T]
  def +=(e1: T, e2: T, es: T*): ISet[T] = { this += e1; this += e2; (this /: es)(_ += _) }

  def -=(e: T): ISet[T]
  def -=(e1: T, e2: T, es: T*): ISet[T] = { this -= e1; this -= e2; (this /: es)(_ -= _) }
}

trait IEvent extends Closeable with ISet[IEventHandler] {
  private[this] val _handlers = new Set[IEventHandler]
  def handlers = _handlers.snapshot

  var oneShot = false
  def emit(source: Any) = handle(source)

  def handle(source: Any) {
    handlers.foreach(_.call(this, source))
    if(oneShot) close()
  }

  override def close { _handlers.foreach(this -= _); super.close() }

  override def +=(h: IEventHandler) = { h.link(this); _handlers += h; this }
  override def -=(h: IEventHandler) = { h.unlink(this); _handlers -= h; this }
}

class Event extends IEvent with IEventHandler
