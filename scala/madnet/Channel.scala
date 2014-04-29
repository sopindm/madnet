package madnet.channel

class Channel extends evil_ant.Closeable {
  override def isOpen: Boolean = true
  override def close(): Unit = { throw new UnsupportedOperationException }

  def readable: Boolean = true
  def closeRead(): Unit = { throw new UnsupportedOperationException }

  def writeable: Boolean = true
  def closeWrite(): Unit = { throw new UnsupportedOperationException }

  def tryPush(obj: AnyRef): Boolean = { throw new UnsupportedOperationException }
  def push(obj: AnyRef): Unit = tryPush(obj)
  def pushIn(obj: AnyRef, milliseconds: Long): Boolean = tryPush(obj)

  def tryPop(): Boolean = { throw new UnsupportedOperationException }
  def pop(): Unit = tryPop()
  def popIn(milliseconds: Long): Boolean = tryPop()
}
