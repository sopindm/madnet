package madnet.channel

import evil_ant.IEvent
import evil_ant.Closeable
import evil_ant.ISignal
import evil_ant.Signal
import evil_ant.MultiSignalSet
import scala.annotation.tailrec

final case class Result(val read: Long, val writen: Long)

object Result {
  val Zero = new Result(0, 0)
}

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

  private def registerEvent(e: IEvent, set: MultiSignalSet) = e match {
    case s: ISignal => set.conj(s)
    case _ => ()
  }

  def register(set: MultiSignalSet) { registerEvent(onClose, set); registerEvent(onActive, set) }
}

class ReadableChannel extends Channel {
  def read(ch: Channel) = readImpl(ch) match {
    case null => throw new UnsupportedOperationException
    case r: Result => r
  }

  protected def readImpl(ch: Channel): Result = null

  @tailrec
  private def _pop(): AnyRef = {
    if(onActive != null)
      onActive.emit(this)

    val result = tryPop()
    if(result != null) result else _pop()
  }

  @tailrec
  private def _popUntil(timeout: Long): AnyRef = {
    val result = tryPop()
    if (result != null || System.currentTimeMillis >= timeout) result else _popUntil(timeout)
  }

  def pop(): AnyRef = _pop()
  def popIn(milliseconds: Long): AnyRef = if(onActive != null) {
    onActive.emitIn(this, milliseconds)
    tryPop()
  }
  else
    _popUntil(System.currentTimeMillis + milliseconds)

  def tryPop(): AnyRef = throw new UnsupportedOperationException
}
