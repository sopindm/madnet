package madnet.channel

import evil_ant.IEvent
import evil_ant.Closeable
import evil_ant.ISignal
import evil_ant.Signal
import evil_ant.MultiSignalSet
import scala.annotation.tailrec

final case class Result(val read: Int, val writen: Int) {
  def this(total: Int) = this(total, total)
}

object Result {
  val Zero = new Result(0, 0)
}

trait IChannel extends evil_ant.Closeable {
  override def isOpen: Boolean = true
  override def close(): Unit = {
    if(onClose != null) onClose.synchronized {
      if(onClose.isOpen) onClose.emit(this); onClose.close()
    }
    if(onActive != null) onActive.synchronized {
      if(onActive.isOpen) onActive.close()
    }
    if(isOpen) closeImpl()
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

class Channel extends IChannel

trait IReadableChannel extends IChannel {
  def canRead(ch: IWritableChannel): Boolean = false

  def read(ch: IWritableChannel): Result = {
    if(canRead(ch)) readImpl(ch)
    else if(ch != null && ch.canWrite(this)) ch.writeImpl(this)
    else throw new UnsupportedOperationException
  }

  def readImpl(ch: IWritableChannel) : Result = null

  def pop(): Any
  def popIn(milliseconds: Long): Any
  def tryPop(): Any
}

trait IReadableChannelLike extends IReadableChannel {
  @tailrec
  private def _pop(): Any = {
    if(onActive != null)
      onActive.emit(this)

    val result = tryPop()
    if(result != null) result else _pop()
  }

  @tailrec
  private def _popUntil(timeout: Long): Any = {
    val result = tryPop()
    if (result != null || System.currentTimeMillis >= timeout) result else _popUntil(timeout)
  }

  override def pop(): Any = _pop()
  override def popIn(milliseconds: Long): Any = if(onActive != null) {
    onActive.emitIn(this, milliseconds)
    tryPop()
  }
  else
    _popUntil(System.currentTimeMillis + milliseconds)

  override def tryPop(): Any = throw new UnsupportedOperationException
}

class ReadableChannel extends Channel with IReadableChannelLike

trait IWritableChannel extends IChannel {
  def canWrite(ch: IReadableChannel): Boolean = false

  def write(ch: IReadableChannel): Result = {
    if(canWrite(ch)) writeImpl(ch)
    else if(ch != null && ch.canRead(this)) ch.readImpl(this)
    else throw new UnsupportedOperationException
  }

  def writeImpl(ch: IReadableChannel): Result = null

  def push(obj: Any): Unit
  def pushIn(obj: Any, milliseconds: Long): Boolean
  def tryPush(obj: Any): Boolean
}

trait IWritableChannelLike extends IWritableChannel {
  @tailrec
  private def _push(obj: Any): Unit = {
    if(onActive != null)
      onActive.emit(this)

    val success = tryPush(obj)
    if(success) () else _push(obj)
  }

  @tailrec
  private def _pushUntil(obj: Any, timestamp: Long): Boolean = {
    if(System.currentTimeMillis >= timestamp) return false

    val success = tryPush(obj)
    if(success) true else _pushUntil(obj, timestamp)
  }

  override def push(obj: Any) = _push(obj)

  override def pushIn(obj: Any, milliseconds: Long) = if(onActive != null) {
    onActive.emitIn(this, milliseconds)
    tryPush(obj)
  }
  else
    _pushUntil(obj, System.currentTimeMillis + milliseconds)

  override def tryPush(obj: Any): Boolean = throw new UnsupportedOperationException
}

class WritableChannel extends Channel with IWritableChannelLike

trait IIOChannel extends IChannel with IReadableChannel with IWritableChannel {
  def reader: IReadableChannel = null
  def writer: IWritableChannel = null

  override def close() = {
    if(reader != null) reader.close()
    if(writer != null) writer.close()
    super.close()
  }

  private[this] def requireReader { if(reader == null) throw new UnsupportedOperationException }
  private[this] def requireWriter { if(writer == null) throw new UnsupportedOperationException }

  override def canRead(ch: IWritableChannel) =
    if(reader != null && reader.canRead(ch)) true
    else if(ch != null && ch.canWrite(reader)) true
    else false

  override def canWrite(ch: IReadableChannel) = 
    if(writer != null && writer.canWrite(ch)) true
    else if(ch != null && ch.canRead(writer)) true
    else false

  override def readImpl(ch: IWritableChannel) = 
    if(reader != null && reader.canRead(ch)) reader.readImpl(ch)
    else if(ch != null && ch.canWrite(reader)) ch.writeImpl(reader)
    else null

  override def writeImpl(ch: IReadableChannel) =
    if(writer != null && writer.canWrite(ch)) writer.writeImpl(ch)
    else if(ch != null && ch.canRead(writer)) ch.readImpl(writer)
    else null

  override def push(obj: Any) = { requireWriter; writer.push(obj) }
  override def pushIn(obj: Any, milliseconds: Long) = {
    requireWriter; writer.pushIn(obj, milliseconds) }
  override def tryPush(obj: Any) = { requireWriter; writer.tryPush(obj) }

  override def pop() = { requireReader; reader.pop() }
  override def popIn(milliseconds: Long) = { requireReader; reader.popIn(milliseconds) }
  override def tryPop() = { requireReader; reader.tryPop() }
}

class IOChannel extends Channel with IIOChannel
