package madnet.sequence

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
  private [sequence] def write(ws: WritableByteSequence, rs: ReadableByteSequence) = {
    val size = scala.math.min(ws.size, rs.size)
    val buffer: java.nio.ByteBuffer = rs.buffer.duplicate
    buffer.limit(rs.begin + size)

    ws.buffer.put(buffer)
    rs.drop(size)

    new madnet.channel.Result(size)
  }
}

class ReadableByteSequence(val buffer: java.nio.ByteBuffer) extends NIOSequence(buffer)
    with IReadableSequence {
  override def get(i: Int) = { requireValidIndex(i); buffer.get(begin + i) }

  override def readImpl(ch: madnet.channel.IWritableChannel) = ch match {
    case ws: WritableByteSequence => ByteSequence.write(ws, this)
    case _ => super.readImpl(ch)
  }
}

class WritableByteSequence(val buffer: java.nio.ByteBuffer) extends NIOSequence(buffer)
  with IWritableSequence {
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

  override def writeImpl(ch: madnet.channel.IReadableChannel) = ch match {
    case rs: ReadableByteSequence => ByteSequence.write(this, rs)
    case _ => super.writeImpl(ch)
  }
}

object CharSequence {
  def write(dst: WritableCharSequence, src: ReadableCharSequence) = {
    val size = scala.math.min(dst.size, src.size)
    val buffer = src.buffer.duplicate
    buffer.limit(src.begin + size)

    dst.buffer.put(buffer)
    src.drop(size)

    new madnet.channel.Result(size)
  }
}

class ReadableCharSequence(val buffer: java.nio.CharBuffer) extends NIOSequence(buffer)
    with IReadableSequence {
  override def get(i: Int) = {
    requireValidIndex(i)
    buffer.get(begin + i)
  }

  override def readImpl(ch: madnet.channel.IWritableChannel) = ch match {
    case ws: WritableCharSequence => CharSequence.write(ws, this)
    case _ => super.readImpl(ch)
  }
}

class WritableCharSequence(val buffer: java.nio.CharBuffer) extends NIOSequence(buffer)
    with IWritableSequence {
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

  override def writeImpl(ch: madnet.channel.IReadableChannel) = ch match {
    case rs: ReadableCharSequence => CharSequence.write(this, rs)
    case _ => super.writeImpl(ch)
  }
}
