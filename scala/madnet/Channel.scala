package madnet.channel

import evil_ant.IEvent
import evil_ant.Closeable
import evil_ant.ISignal
import evil_ant.Signal
import evil_ant.MultiSignalSet
import scala.annotation.tailrec

final case class Result(val read: Int, val writen: Int) {
  def this(readAndWriten: Int) = this(readAndWriten, readAndWriten)
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

trait IInputChannel extends IChannel {
  def pop(): Any
  def popIn(milliseconds: Long): Any
  def tryPop(): Any
}

trait IInputChannelLike extends IInputChannel {
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

class InputChannel extends Channel with IInputChannelLike

trait IOutputChannel extends IChannel {
  def push(obj: Any): Unit
  def pushIn(obj: Any, milliseconds: Long): Boolean
  def tryPush(obj: Any): Boolean
}

trait IOutputChannelLike extends IOutputChannel {
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

class OutputChannel extends Channel with IOutputChannelLike

trait IIOChannel extends IChannel with IInputChannel with IOutputChannel {
  def reader: IInputChannel = null
  def writer: IOutputChannel = null

  override def close() = {
    if(reader != null) reader.close()
    if(writer != null) writer.close()
    super.close()
  }

  private[this] def requireReader { if(reader == null) throw new UnsupportedOperationException }
  private[this] def requireWriter { if(writer == null) throw new UnsupportedOperationException }

  override def push(obj: Any) = { requireWriter; writer.push(obj) }
  override def pushIn(obj: Any, milliseconds: Long) = {
    requireWriter; writer.pushIn(obj, milliseconds) }
  override def tryPush(obj: Any) = { requireWriter; writer.tryPush(obj) }

  override def pop() = { requireReader; reader.pop() }
  override def popIn(milliseconds: Long) = { requireReader; reader.popIn(milliseconds) }
  override def tryPop() = { requireReader; reader.tryPop() }
}

class IOChannel extends Channel with IIOChannel
