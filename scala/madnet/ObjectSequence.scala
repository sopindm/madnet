package madnet.sequence

class ObjectSequence(protected val buffer: Array[Any], override var begin: Int, override var size: Int)
    extends Sequence {
  require { begin + size <= buffer.size }

  override def freeSpace = buffer.size - begin - size

  protected def requireValidIndex(i: Int) {
    if(i < 0 || i >= size) throw new ArrayIndexOutOfBoundsException
  }
}

class ReadableObjectSequence(buffer: Array[Any], begin: Int, size: Int)
    extends ObjectSequence(buffer, begin, size) with IReadableSequence {
  override def get(i: Int) = { 
    requireValidIndex(i)
    buffer(begin + i)
  }
}

class WritableObjectSequence(buffer: Array[Any], begin: Int, size: Int) 
  extends ObjectSequence(buffer, begin, size) with IWritableSequence {
  override def set(i: Int, value: Any) = {
    requireValidIndex(i)
    if(value == null) throw new IllegalArgumentException
    buffer(begin + i) = value
  }
}

