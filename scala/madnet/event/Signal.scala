package madnet.event;

import java.util.concurrent.TimeUnit

trait ISignal extends IEvent  {
  override def close() { _attachment = null; super.close() }

  def start(): Unit

  var persistent:Boolean

  private var _attachment: AnyRef = null
  def attachment = _attachment
  def attach(obj: AnyRef) { _attachment = obj }
}

trait ISignalSet[T <: ISignal] extends ISet[T] with java.io.Closeable {
  def signals: scala.collection.immutable.Set[T]
  def selections: scala.collection.mutable.Set[T]

  def select: Unit
  def selectIn(milliseconds: Int): Unit
  def selectNow: Unit
}

trait ISignalSetLike[T <: ISignal] extends ISignalSet[T] {
  protected val _signals = new Set[T]
  override def signals = _signals.snapshot

  override def +=(signal: T) = {
    requireOpen
    _signals += signal
    this
  }

  override def -=(signal: T) = { _signals -= signal; this }

  def isEmpty: Boolean = signals.isEmpty

  private[this] var _isOpen = true
  def isOpen = _isOpen

  protected def requireOpen {
    if(!isOpen) throw new java.nio.channels.ClosedSelectorException
  }

  override def close { signals.foreach(this -= _); _isOpen = false }

  override val selections = new Set[T]
}

class TriggerSignal extends ISignal {
  private var _provider: TriggerSet = null
  def provider = _provider

  override def close() {
    if(provider != null) provider -= this
    super.close()
  }

  private[event] def register(provider: TriggerSet) {
    require(_provider == null)
    _provider = provider
  }

  override var persistent = false
  override def emit(obj: Any) {
    super.emit(obj)
    if(persistent) start()
  }

  private[event] def cancel { _provider = null }

  override def start { provider.trigger(this) }
}


class TriggerSet extends ISignalSetLike[TriggerSignal] {
  private[this] val triggered = new java.util.concurrent.LinkedBlockingQueue[TriggerSignal]()
  private[event] def trigger(s: TriggerSignal) { triggered.add(s) }

  private def _push(s: TriggerSignal) { if(s != null) selections += s }

  override def select {
    requireOpen
    if(!isEmpty) _push(triggered.take())
    selectNow
  }

  override def selectIn(millis: Int) {
    requireOpen
    if(!isEmpty) _push(triggered.poll(millis, TimeUnit.MILLISECONDS));
    selectNow
  }

  override def selectNow {
    requireOpen
    while(triggered.size() > 0)
      _push(triggered.poll())
 }

  override def +=(signal: TriggerSignal) = {
    signal.register(this); super.+=(signal)
  }

  override def -=(signal: TriggerSignal) = {
    signal.cancel; super.-=(signal)
  }
}
