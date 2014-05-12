package madnet.sequence

trait ISequence extends madnet.channel.Channel {
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
  def drop(n: Int): Unit = { requireSize(n); begin += n; size -= n }
  def expand(n: Int): Unit = { requireFreeSpace(n); size += n }
}

class Sequence extends ISequence

class ReadableSequence extends madnet.channel.ReadableChannel with ISequence
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
}
