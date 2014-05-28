package madnet.sequence

trait ISequence extends madnet.channel.IChannel with Cloneable {
  override def clone: ISequence = throw new UnsupportedOperationException

  def begin: Int = throw new UnsupportedOperationException
  protected def begin_=(n: Int): Unit = throw new UnsupportedOperationException

  def end: Int = begin + size

  def size: Int = throw new UnsupportedOperationException
  protected def size_=(n: Int): Unit = throw new UnsupportedOperationException

  def freeSpace: Int = throw new UnsupportedOperationException

  private[this] def requireSize(n: Int) =
    if(size < n) throw new java.nio.BufferUnderflowException
  private[this] def requireFreeSpace(n: Int) =
    if(freeSpace < n) throw new java.nio.BufferOverflowException

  def take(n: Int): Unit = { requireSize(n);  size = n }
  def drop(n: Int): Unit = { requireSize(n); size -= n; begin += n }
  def expand(n: Int): Unit = { requireFreeSpace(n); size += n }
}

class Sequence extends madnet.channel.Channel with ISequence

trait IReadableSequence extends ISequence with madnet.channel.IReadableChannelLike
    with java.lang.Iterable[Any] {
  def get(n: Int): Any = throw new UnsupportedOperationException

  override def tryPop(): Any = { 
    if(size == 0) return null
    val value = get(0); drop(1); value
  }

  override def iterator(): java.util.Iterator[Any] = new java.util.Iterator[Any] {
    var position = 0

    override def hasNext() = position < size
    override def next() = { val current = get(position); position += 1; current }
    override def remove() { throw new UnsupportedOperationException }
  }

  override def canRead(ch: madnet.channel.IWritableChannel) = ch match {
    case s: WritableSequence => true
    case _ => false
  }

  override def readImpl(ch: madnet.channel.IWritableChannel) = ch match {
    case w: WritableSequence => Sequence.write(w, this)
    case _ => null
  }
}

class ReadableSequence extends Sequence with IReadableSequence

object Sequence {
  private[sequence] 
  def write(to: IWritableSequence, from: IReadableSequence) = {
    val size = scala.math.min(to.size, from.size)

    for(i <- 0 until size) to.set(i, from.get(i))
    from.drop(size)
    to.drop(size)

    new madnet.channel.Result(size)
  }
}

trait IWritableSequence extends ISequence with madnet.channel.IWritableChannelLike {
  def set(n: Int, value: Any): Unit = throw new UnsupportedOperationException

  override def tryPush(value: Any): Boolean = if(size == 0) false else {
    set(0, value)
    drop(1)
    true
  }

  override def canWrite(ch: madnet.channel.IReadableChannel) = ch match {
    case s: ReadableSequence => true
    case _ => false
  }

  override def writeImpl(ch: madnet.channel.IReadableChannel) = ch match {
    case r: ReadableSequence => Sequence.write(this, r)
    case _ => null
  }
}

class WritableSequence extends Sequence with IWritableSequence

class IOSequence(val linked: Boolean) extends Sequence with madnet.channel.IIOChannel
    with java.lang.Iterable[Any] {
  override def reader: ReadableSequence = throw new UnsupportedOperationException
  override def writer: WritableSequence = throw new UnsupportedOperationException

  override def begin = reader.begin
  override def size = reader.size
  override def freeSpace = writer.freeSpace

  override def take(n: Int) { throw new UnsupportedOperationException }
  override def drop(n: Int) = { reader.drop(n); if(linked) writer.expand(n) }
  override def expand(n: Int) = { writer.expand(n); if(linked) reader.drop(n) }

  override def iterator = reader.iterator

  override def tryPush(o: Any) = if(writer.tryPush(o)) { reader.expand(1); true } else false

  override def writeImpl(ch: madnet.channel.IReadableChannel) = {
    val result = super.writeImpl(ch)
    if(result != null && reader != null) reader.expand(result.writen)

    result
  }

  override def readImpl(ch: madnet.channel.IWritableChannel) = {
    val result = super.readImpl(ch)
    if(result != null && writer != null && linked) writer.expand(result.read)

    result
  }
}
