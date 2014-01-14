package madnet.sequence;

import madnet.util.Pair;

public class CircularSequence extends ISequence
{
    private ASequence reader;
    private ASequence writer;

    public CircularSequence(ASequence sequence)
    {
        if(sequence.buffer() == null)
            throw new IllegalArgumentException("Sequence must have buffer");

        reader = sequence;
        writer = null;
    }

    private CircularSequence(ASequence reader, ASequence writer)
    {
        this.reader = reader;
        this.writer = writer;
    }

    public IBuffer buffer()
    {
        return reader.buffer();
    }

    public int size()
    {
        if(writer != null)
            return reader.size() + writer.size();

        return reader.size();
    }

    public int freeSpace()
    {
        if(writer != null)
            return reader.buffer().size() - reader.size() - writer.size();

        return reader.buffer().size() - reader.size();
    }

    public ISequence take(int n)
    {
        if(reader.size() >= n)
            return new CircularSequence(reader.take(n), null);

        return new CircularSequence(reader, writer.take(n - reader.size()));
    }

    public ISequence drop(int n)
    {
        if(reader.size() >= n)
            return new CircularSequence(reader.drop(n), writer);

        return new CircularSequence(writer.drop(n - reader.size()), null);
    }

    public ISequence expand(int n)
    {
        if(reader.freeSpace() >= n)
            return new CircularSequence((ASequence)reader.expand(n));

        if(writer != null)
            return new CircularSequence(reader, (ASequence)writer.expand(n));

        return new CircularSequence(reader.expand(reader.freeSpace()),
                                    (ASequence)reader.buffer().sequence(0, n - reader.freeSpace()));
    }

    public Pair<ISequence, ISequence> write(ISequence seq) 
    {
        throw new UnsupportedOperationException();
    }

    public Pair<ISequence, Iterable<ISequence>> write(Iterable<ISequence> seq) 
    {
        throw new UnsupportedOperationException();
    }

    public Pair<ISequence, ISequence> writeImpl(ISequence seq)
    {
        return write(seq);
    }

    public Pair<ISequence, ISequence> read(ISequence seq) 
    {
        throw new UnsupportedOperationException();
    }

    public Pair<ISequence, Iterable<ISequence>> read(Iterable<ISequence> seq) 
    {
        throw new UnsupportedOperationException();
    }

    public Pair<ISequence, ISequence> readImpl(ISequence seq)
    {
        return read(seq);
    }
}