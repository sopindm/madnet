package madnet.sequence

import java.nio.charset.Charset
import java.nio.charset.CharsetDecoder
import java.nio.charset.CharsetEncoder

class NIOSequence(buffer: java.nio.Buffer) extends Sequence {
  override def begin = buffer.position
  override def begin_=(i: Int) = {
    buffer.limit(i + size)
    buffer.position(i)
  }

  override def size = buffer.limit - begin
  override def size_=(i: Int) = buffer.limit(i + begin)

  override def freeSpace = buffer.capacity - buffer.limit

  protected def requireValidIndex(i: Int) {
    if(i < 0 || i >= size) throw new ArrayIndexOutOfBoundsException
  }
}

object ByteSequence {
  def write(ws: OutputByteSequence, rs: InputByteSequence) = {
    val size = scala.math.min(ws.size, rs.size)
    val buffer: java.nio.ByteBuffer = rs.buffer.duplicate
    buffer.limit(rs.begin + size)

    ws.buffer.put(buffer)
    rs.drop(size)

    new madnet.channel.Result(size)
  }
}

class InputByteSequence(val buffer: java.nio.ByteBuffer) extends NIOSequence(buffer)
    with IInputSequence {
  override def get(i: Int) = { requireValidIndex(i); buffer.get(begin + i) }
}

class OutputByteSequence(val buffer: java.nio.ByteBuffer) extends NIOSequence(buffer)
  with IOutputSequence {
  override def set(i: Int, v: Any) = {
    requireValidIndex(i)
    try {
      val b: Byte = v.asInstanceOf[Byte]
      buffer.put(begin + i, b)
    }
    catch {
      case e: ClassCastException => throw new IllegalArgumentException
    }
  }
}

object CharSequence {
  def write(dst: OutputCharSequence, src: InputCharSequence) = {
    val size = scala.math.min(dst.size, src.size)
    val buffer = src.buffer.duplicate
    buffer.limit(src.begin + size)

    dst.buffer.put(buffer)
    src.drop(size)

    new madnet.channel.Result(size)
  }

  def writeBytes(dst: OutputByteSequence, src: InputCharSequence, charset: Charset) = {
    val charBegin = src.begin
    val byteBegin = dst.begin

    val result = charset.newEncoder.encode(src.buffer, dst.buffer, true)
    if(result.isError) throw new java.nio.charset.CharacterCodingException

    new madnet.channel.Result(src.begin - charBegin, dst.begin - byteBegin)
  }

  def readBytes(src: InputByteSequence, dst: OutputCharSequence, charset: Charset) = {
    val charBegin = dst.begin
    val byteBegin = src.begin

    val result = charset.newDecoder.decode(src.buffer, dst.buffer, true)
    if(result.isError) throw new java.nio.charset.CharacterCodingException

    new madnet.channel.Result(src.begin - byteBegin, dst.begin - byteBegin)
  }
}

class InputCharSequence(val buffer: java.nio.CharBuffer) extends NIOSequence(buffer)
    with IInputSequence {
  override def get(i: Int) = {
    requireValidIndex(i)
    buffer.get(begin + i)
  }
}

class OutputCharSequence(val buffer: java.nio.CharBuffer) extends NIOSequence(buffer)
    with IOutputSequence {
  override def set(i: Int, value: Any )  = {
    requireValidIndex(i)
    try {
      val b = value.asInstanceOf[Character]
      buffer.put(begin + i, b)
    }
    catch {
      case e: ClassCastException => throw new IllegalArgumentException
    }
  }
}
