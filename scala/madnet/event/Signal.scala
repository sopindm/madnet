package madnet.event;

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
    requireOpen()
    _signals += signal
    this
  }

  override def -=(signal: T) = { _signals -= signal; this }

  def isEmpty: Boolean = signals.isEmpty

  private[this] var _isOpen = true
  def isOpen = _isOpen

  protected def requireOpen() {
    if(!isOpen) throw new java.nio.channels.ClosedSelectorException
  }

  override def close { signals.foreach(this -= _); _isOpen = false }

  override val selections = new Set[T]
}
