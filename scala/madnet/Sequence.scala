package madnet.sequence

abstract class Buffer {
  def size: Int

  def get(index: Int): Any
  def set(index: Int, value: Any): Unit

  def copy(fromIndex: Int, toIndex: Int, size: Int)
}

trait ISequence extends madnet.channel.IChannel with Cloneable {
  override def clone: ISequence = throw new UnsupportedOperationException

  def begin: Int = throw new UnsupportedOperationException
  private[sequence] def begin_=(n: Int): Unit = throw new UnsupportedOperationException

  def end: Int = begin + size

  def size: Int = throw new UnsupportedOperationException
  private[sequence] def size_=(n: Int): Unit = throw new UnsupportedOperationException

  def freeSpace: Int = throw new UnsupportedOperationException

  private[this] def requireSize(n: Int) =
    if(size < n) throw new java.nio.BufferUnderflowException
  private[this] def requireFreeSpace(n: Int) =
    if(freeSpace < n) throw new java.nio.BufferOverflowException

  def take(n: Int): Unit = { requireSize(n);  size = n }
  def drop(n: Int): Unit = { requireSize(n); size -= n; begin += n }
  def expand(n: Int): Unit = { requireFreeSpace(n); size += n }

  protected def requireValidIndex(i: Int) {
    if(i < 0 || i >= size) throw new ArrayIndexOutOfBoundsException
  }

  def buffer: Buffer
}

class Sequence extends madnet.channel.Channel with ISequence {
  override def buffer: Buffer = throw new UnsupportedOperationException
}

trait IInputSequence extends ISequence with madnet.channel.IInputChannelLike
    with java.lang.Iterable[Any] {
  def get(n: Int): Any = { requireValidIndex(n); buffer.get(begin + n) }

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

class InputSequence extends Sequence with IInputSequence

trait IOutputSequence extends ISequence with madnet.channel.IOutputChannelLike {
  def set(n: Int, value: Any): Unit = {
    requireValidIndex(n)
    if(value == null) throw new IllegalArgumentException
    buffer.set(begin + n, value)
  }

  override def tryPush(value: Any): Boolean = if(size == 0) false else {
    set(0, value)
    drop(1)
    true
  }
}

object Sequence {
  def write(dst: IOutputSequence, src: IInputSequence) = {
    val size = scala.math.min(src.size, dst.size)

    for(i <- 0 until size) dst.set(i, src.get(i))
    src.drop(size)
    dst.drop(size)

    new madnet.channel.Result(size)
  }
}

class OutputSequence extends Sequence with IOutputSequence

class IOSequence(val linked: Boolean) extends Sequence with madnet.channel.IIOChannel
    with java.lang.Iterable[Any] {
  override def reader: IInputSequence = throw new UnsupportedOperationException
  override def writer: IOutputSequence = throw new UnsupportedOperationException

  override def begin = reader.begin
  override def size = reader.size
  override def freeSpace =
    if(linked) scala.math.min(writer.freeSpace, reader.size)
    else writer.freeSpace

  override def take(n: Int) { throw new UnsupportedOperationException }
  override def drop(n: Int) = { reader.drop(n); if(linked) writer.expand(n) }
  override def expand(n: Int) = { writer.expand(n); if(linked) reader.drop(n) }

  override def iterator = reader.iterator

  override def tryPush(o: Any) = if(writer.tryPush(o)) { reader.expand(1); true } else false
}



