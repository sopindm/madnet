package madnet.sequence;

import java.util.Iterator;
import java.util.ArrayList;
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
        if(reader.size() == 0 && writer != null)
        {
            this.reader = writer;
            this.writer = null;
        }
        else
        {
            this.reader = reader;
            this.writer = writer;
        }
    }

    public ASequence[] sequencies() 
    {
        if(writer != null)
        {
            ASequence[] ret = {reader, writer};
            return ret;
        }

        ASequence[] ret = {reader};
        return ret;
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
        if(writer == null) {
            Pair<ISequence, ISequence> writen = reader.write(seq);
            return new Pair<ISequence, ISequence>
                (new CircularSequence((ASequence)writen.first, null),
                 writen.second);
        }

        Pair<ISequence, ISequence> writen = writer.write(seq);

        return new Pair<ISequence, ISequence>
            (new CircularSequence(reader, (ASequence)writen.first),
             writen.second);
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
        if(writer != null)
        {
            ArrayList<ISequence> seqs = new ArrayList<ISequence>(2);
            seqs.add(reader);
            seqs.add(writer);

            Pair<ISequence, Iterable<ISequence>> writen = seq.write(seqs);
            Iterator<ISequence> iter = writen.second.iterator();

            ASequence newReader = (ASequence)iter.next();
            ASequence newWriter = (ASequence)iter.next();

            return new Pair<ISequence, ISequence>
                (new CircularSequence(newReader, newWriter),
                 writen.first);
        }

        Pair<ISequence, ISequence> read = reader.read(seq);

        return new Pair<ISequence, ISequence>
            (new CircularSequence((ASequence)read.first, null),
             read.second);
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