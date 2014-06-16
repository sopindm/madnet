package madnet.sequence

import java.nio.charset.Charset
import java.nio.charset.CharsetDecoder
import java.nio.charset.CharsetEncoder

abstract class NIOBuffer(val buffer: java.nio.Buffer) extends Buffer {
  override def size = buffer.capacity
}

class NIOSequence(_buffer: java.nio.Buffer) extends Sequence {
  override def buffer: NIOBuffer = throw new UnsupportedOperationException

  override def begin = _buffer.position
  override def begin_=(i: Int) = {
    _buffer.limit(i + size)
    _buffer.position(i)
  }

  override def size = _buffer.limit - begin
  override def size_=(i: Int) = _buffer.limit(i + begin)

  override def freeSpace = _buffer.capacity - _buffer.limit
}

object ByteSequence {
  def write(ws: OutputByteSequence, rs: InputByteSequence) = {
    val size = scala.math.min(ws.size, rs.size)
    val buffer: java.nio.ByteBuffer = rs.buffer.buffer.duplicate
    buffer.limit(rs.begin + size)

    ws.buffer.buffer.put(buffer)
    rs.drop(size)

    new madnet.channel.Result(size)
  }
}

class ByteBuffer(override val buffer: java.nio.ByteBuffer) extends NIOBuffer(buffer) {
  override def get(index: Int) = buffer.get(index)
  override def set(index: Int, value: Any) = value match {
    case b: Byte => buffer.put(index, b)
    case _ => throw new IllegalArgumentException
  }

  override def copy(fromIndex: Int, toIndex: Int, size: Int) = {
    val src = buffer.duplicate; src.position(fromIndex).limit(fromIndex + size)
    val dst = buffer.duplicate; dst.position(toIndex).limit(toIndex + size)

    dst.put(src)
  }
}

class ByteSequence(_buffer: java.nio.ByteBuffer) extends NIOSequence(_buffer) { 
  override val buffer = new ByteBuffer(_buffer)
}

class InputByteSequence(_buffer: java.nio.ByteBuffer)
    extends ByteSequence(_buffer) with IInputSequence

class OutputByteSequence(_buffer: java.nio.ByteBuffer)
    extends ByteSequence(_buffer) with IOutputSequence

object CharSequence {
  def write(dst: OutputCharSequence, src: InputCharSequence) = {
    val size = scala.math.min(dst.size, src.size)
    val buffer = src.buffer.buffer.duplicate
    buffer.limit(src.begin + size)

    dst.buffer.buffer.put(buffer)
    src.drop(size)

    new madnet.channel.Result(size)
  }

  def writeBytes(dst: OutputByteSequence, src: InputCharSequence, charset: Charset) = {
    val charBegin = src.begin
    val byteBegin = dst.begin

    val result = charset.newEncoder.encode(src.buffer.buffer, dst.buffer.buffer, true)
    if(result.isError) throw new java.nio.charset.CharacterCodingException

    new madnet.channel.Result(src.begin - charBegin, dst.begin - byteBegin)
  }

  def readBytes(src: InputByteSequence, dst: OutputCharSequence, charset: Charset) = {
    val charBegin = dst.begin
    val byteBegin = src.begin

    val result = charset.newDecoder.decode(src.buffer.buffer, dst.buffer.buffer, true)
    if(result.isError) throw new java.nio.charset.CharacterCodingException

    new madnet.channel.Result(src.begin - byteBegin, dst.begin - byteBegin)
  }
}

class CharBuffer(override val buffer: java.nio.CharBuffer) extends NIOBuffer(buffer) {
  override def get(index: Int) = buffer.get(index)
  override def set(index: Int, value: Any) = value match {
    case c: Char => buffer.put(index, c)
    case _ => throw new IllegalArgumentException
  }

  override def copy(fromIndex: Int, toIndex: Int, size: Int) = {
    val src = buffer.duplicate; src.position(fromIndex).limit(fromIndex + size)
    val dst = buffer.duplicate; dst.position(toIndex).limit(toIndex + size)

    dst.put(src)
  }
}

class CharSequence(_buffer: java.nio.CharBuffer) extends NIOSequence(_buffer) {
  override val buffer = new CharBuffer(_buffer)
}

class InputCharSequence(_buffer: java.nio.CharBuffer)
    extends CharSequence(_buffer) with IInputSequence

class OutputCharSequence(_buffer: java.nio.CharBuffer)
    extends CharSequence(_buffer) with IOutputSequence
