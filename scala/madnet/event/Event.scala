package madnet.event

class Set[T] extends scala.collection.mutable.Set[T] {
  private [this] var _set = scala.collection.immutable.Set[T]()
  def snapshot = _set

  override def +=(e: T) = { _set += e; this }
  override def -=(e: T) = { _set -= e; this }
  override def contains(e: T) = _set.contains(e)
  override def iterator = _set.iterator
}

class EventHandler {
  private[this] val _emitters = new Set[Event]
  def emitters = _emitters.snapshot

  def call(emitter: Event, source: Any) {}

  private[event] def link(e: Event) { _emitters += e }
}

trait ISet[T] {
  def +=(e: T): ISet[T]
  def +=(e1: T, e2: T, es: T*): ISet[T] = { this += e1; this += e2; (this /: es)(_ += _) }
}

class Event extends ISet[EventHandler] {
  private[this] val _handlers = new Set[EventHandler]
  def handlers = _handlers.snapshot

  def emit(source: Any) { handlers.foreach(_.call(this, source)) }

  override def +=(h: EventHandler) = { h.link(this); _handlers += h; this }
}
