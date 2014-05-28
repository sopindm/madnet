package madnet.sequence

class NIOSequence(buffer: java.nio.Buffer) extends Sequence {
  override def begin = buffer.position
  override def begin_=(i: Int) = {
    val lostSize = size
    buffer.position(i)
    size = lostSize
  }

  override def size = buffer.limit - begin
  override def size_=(i: Int) = buffer.limit(i + begin)

  override def freeSpace = buffer.capacity - buffer.limit
}
