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

trait IReadableChannel extends Channel {
  def read(ch: Channel): Result

  def pop(): AnyRef
  def popIn(milliseconds: Long): AnyRef
  def tryPop(): AnyRef
}

class ReadableChannel extends Channel with IReadableChannel {
  override def read(ch: Channel) = readImpl(ch) match {
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

  override def pop(): AnyRef = _pop()
  override def popIn(milliseconds: Long): AnyRef = if(onActive != null) {
    onActive.emitIn(this, milliseconds)
    tryPop()
  }
  else
    _popUntil(System.currentTimeMillis + milliseconds)

  override def tryPop(): AnyRef = throw new UnsupportedOperationException
}

trait IWritableChannel extends Channel{
  def write(ch: Channel): Result

  def push(obj: AnyRef): Unit
  def pushIn(obj: AnyRef, milliseconds: Long): Boolean
  def tryPush(obj: AnyRef): Boolean
}

class WritableChannel extends Channel with IWritableChannel {
  override def write(ch: Channel) = writeImpl(ch) match {
    case null => throw new UnsupportedOperationException
    case r: Result => r
  }

  def writeImpl(ch: Channel): Result = null
  
  @tailrec
  private def _push(obj: AnyRef): Unit = {
    if(onActive != null)
      onActive.emit(this)

    val success = tryPush(obj)
    if(success) () else _push(obj)
  }

  @tailrec
  private def _pushUntil(obj: AnyRef, timestamp: Long): Boolean = {
    if(System.currentTimeMillis >= timestamp) return false

    val success = tryPush(obj)
    if(success) true else _pushUntil(obj, timestamp)
  }

  override def push(obj: AnyRef) = _push(obj)

  def pushIn(obj: AnyRef, milliseconds: Long) = if(onActive != null) {
    onActive.emitIn(this, milliseconds)
    tryPush(obj)
  }
  else
    _pushUntil(obj, System.currentTimeMillis + milliseconds)

  def tryPush(obj: AnyRef): Boolean = throw new UnsupportedOperationException
}
