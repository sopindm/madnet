package madnet.event;

import java.util.concurrent.TimeUnit

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
    requireOpen()
    if(!isEmpty) _push(triggered.take())
    selectNow
  }

  override def selectIn(millis: Int) {
    requireOpen()
    if(!isEmpty) _push(triggered.poll(millis, TimeUnit.MILLISECONDS));
    selectNow
  }

  override def selectNow {
    requireOpen()
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
