package madnet.sequence

class ObjectBuffer(buffer: Array[Any]) extends Buffer {
  override def size = buffer.size

  override def get(i: Int) = buffer(i)
  override def set(i: Int, v: Any) { buffer(i) = v }

  override def copy(from: Int, to: Int, size: Int) {
    for(i <- 0 until size) buffer(to + i) = buffer(from + i)
  }
}

class ObjectSequence(_buffer: Array[Any], override var begin: Int, override var size: Int)
    extends Sequence {
  require { begin + size <= _buffer.size }

  override val buffer = new ObjectBuffer(_buffer)
  override def freeSpace = buffer.size - begin - size
}

class InputObjectSequence(_buffer: Array[Any], begin: Int, size: Int)
    extends ObjectSequence(_buffer, begin, size) with IInputSequence

class OutputObjectSequence(_buffer: Array[Any], begin: Int, size: Int) 
  extends ObjectSequence(_buffer, begin, size) with IOutputSequence
