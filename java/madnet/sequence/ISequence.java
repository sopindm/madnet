package madnet.sequence;

import madnet.util.Pair;

public abstract class ISequence
{
    abstract public IBuffer buffer();

    abstract public int size();
    abstract public int freeSpace();

    abstract public int limit();
    abstract public ISequence limit(int newLimit);

    abstract public ISequence take(int n);
    abstract public ISequence drop(int n);

    abstract public ISequence expand(int size);

    abstract public Pair<ISequence, ISequence> read(ISequence seq);
    abstract public Pair<ISequence, Iterable<ISequence>> read(Iterable<ISequence> seq);

    abstract public Pair<ISequence, ISequence> write(ISequence seq);
    abstract public Pair<ISequence, Iterable<ISequence>> write(Iterable<ISequence> seq);

    abstract protected Pair<ISequence, ISequence> readImpl(ISequence seq);
    abstract protected Pair<ISequence, ISequence> writeImpl(ISequence seq);
}
