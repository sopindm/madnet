package madnet.event;

import java.util.concurrent.TimeUnit

trait ISignal {
  def start: Unit
}

trait ISignalSet[T <: ISignal] extends ISet[T] {
  def signals: scala.collection.immutable.Set[T]
  def selections: scala.collection.mutable.Set[T]

  def select: Unit
  def selectIn(milliseconds: Int): Unit
  def selectNow: Unit
}

trait ISignalSetLike[T <: ISignal] extends ISignalSet[T] {
  protected val _signals = new Set[T]
  override def signals = _signals.snapshot

  override val selections = new Set[T]

  override def +=(signal: T) = { _signals += signal; this }
  override def -=(signal: T) = { _signals -= signal; this }
}

class TriggerSignal extends ISignal {
  private var _provider: TriggerSet = null
  def provider = _provider

  private[event] def register(provider: TriggerSet) {
    require(_provider == null)
    _provider = provider
  }

  override def start { provider.trigger(this) }
}


class TriggerSet extends ISignalSetLike[TriggerSignal] {
  private[this] val triggered = new java.util.concurrent.LinkedBlockingQueue[TriggerSignal]()
  private[event] def trigger(s: TriggerSignal) { triggered.add(s) }

  private def _push(s: TriggerSignal) { if(s != null) selections += s }

  override def select { _push(triggered.take()); selectNow }
  override def selectIn(millis: Int) { _push(triggered.poll(millis, TimeUnit.MILLISECONDS)); selectNow }
  override def selectNow { while(triggered.size() > 0) _push(triggered.poll()) }

  override def +=(signal: TriggerSignal) = { signal.register(this); super.+=(signal) }
}
