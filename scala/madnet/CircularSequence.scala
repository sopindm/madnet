package madnet.sequence

class CircularSequence private ( val head: ISequence,
                                 val compactionThreshold: Int,
                                 var tail: Int)
    extends Sequence with ICircularSequence {
  def this(_head: ISequence, _compactionThreshold: Int) =
    this(_head, _compactionThreshold, _head.begin + _head.size - _head.buffer.size)

  require { freeSpace >= 0 }

  def this(head: ISequence) = this(head, 0)

  override def buffer = head.buffer

  override def begin = head.begin
  override def begin_=(_n: Int) {
    var n = _n % buffer.size
    val oldSize = size
    if(n + head.size > buffer.size) head.size = buffer.size - n
    head.begin = n
    tail = head.begin + oldSize - buffer.size
  }

  override def end = if(begin + size >= buffer.size) 
    (begin + size + compactionThreshold) % buffer.size
  else
    (begin + size) % buffer.size

  override def size = tail + buffer.size - head.begin

  override def size_=(n: Int) {
    head.size = scala.math.min(n, buffer.size - head.begin)
    tail = head.begin + n - buffer.size
  }

  override def freeSpace = buffer.size - size - compactionThreshold

  override def clone = new CircularSequence(head.clone, compactionThreshold, tail)

  def needCompaction = (head.begin >= buffer.size - compactionThreshold &&
                        head.begin + head.size == buffer.size)

  def compact = if(needCompaction) {
    val oldSize = size
    head.begin = head.begin - (buffer.size - compactionThreshold)
    head.size = oldSize
    tail = head.begin + head.size - buffer.size
  }

  override def drop(n: Int) {
    if(head.begin + n >= buffer.size) { begin = begin + compactionThreshold }
    super.drop(n)
    compact
  }
}

class InputCircularSequence(head: ISequence, compactionThreshold: Int)
    extends CircularSequence(head, compactionThreshold) with IInputSequence {
  def this(head: ISequence) = this(head, 0)

  override def get(n: Int) = {
    requireValidIndex(n)
    buffer.get((begin + n) % buffer.size)
  }

  override def compact {
    if(needCompaction) 
      for(i <- head.begin until head.end)
        buffer.set(i - (buffer.size - compactionThreshold), buffer.get(i))

    super.compact
  }
}

class OutputCircularSequence(head: ISequence, compactionThreshold: Int)
    extends CircularSequence(head, compactionThreshold) with IOutputSequence {
  def this(head: ISequence) = this(head, 0)

  override def set(n: Int, v: Any) = {
    requireValidIndex(n)
    buffer.set((begin + n) % buffer.size, v)
  }

  override def drop(n: Int) {
    if(head.begin < compactionThreshold)
      for(i <- head.begin until scala.math.min(compactionThreshold, head.begin + n))
        buffer.set(buffer.size - compactionThreshold + i, buffer.get(i))
    super.drop(n)
  }
}
