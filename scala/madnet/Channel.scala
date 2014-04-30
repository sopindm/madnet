package madnet.channel
import evil_ant.IEvent
import evil_ant.Closeable

class Channel extends evil_ant.Closeable {
  override def isOpen: Boolean = true
  override def close(): Unit = {
    if(onClose != null) { onClose.emit(this); onClose.close() }
    if(onActive != null) { onActive.close() }
    closeImpl()
  }
  protected def closeImpl(): Unit = { throw new UnsupportedOperationException }
  def onClose: IEvent = null

  def isActive: Boolean = true
  def onActive: IEvent = null
}
