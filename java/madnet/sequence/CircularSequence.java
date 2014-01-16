package madnet.sequence;

import java.util.Iterator;
import java.util.ArrayList;
import madnet.util.Pair;

public class CircularSequence extends ISequence
{
    final private ASequence reader;
    final private ASequence writer;

    public CircularSequence(ASequence sequence)
    {
        if(sequence.buffer() == null)
            throw new IllegalArgumentException("Sequence must have buffer");

        reader = sequence;
        writer = (ASequence)sequence.buffer().sequence(0, 0, sequence.position());
    }

    private CircularSequence(ASequence reader, ASequence writer)
    {
        if(reader.size() == 0 && reader.freeSpace() == 0)
        {
            this.reader = writer;
            this.writer = (ASequence)reader.buffer().sequence(0, 0, writer.position());
        }
        else
        {
            this.reader = reader;
            this.writer = writer;
        }
    }

    public int limit() 
    {
        if(reader.limit() < buffer().size())
            return reader.limit();

        return reader.limit() - reader.position() + writer.limit();
    }

    public CircularSequence limit(int newLimit)
    {
        int readerRemains = buffer().size() - reader.position();

        if(newLimit < readerRemains)
            return new CircularSequence(reader.limit(reader.position() + newLimit),
                                        writer.limit(0));

        return new CircularSequence(reader.limit(buffer().size()),
                                    writer.limit(newLimit - readerRemains));
    }

    public ASequence[] sequencies() 
    {
        if(writer.size() > 0)
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
            return reader.freeSpace() + writer.freeSpace();

        return reader.freeSpace() + reader.position();
    }

    public ISequence take(int n)
    {
        int readerTake = Math.min(n, reader.size());
        int writerTake = n - readerTake;

        return new CircularSequence(reader.take(readerTake), writer.take(writerTake));
    }

    public ISequence drop(int n)
    {
        int readerDrop = Math.min(n, reader.size());
        int writerDrop = n - readerDrop;

        int writerLimit = writer.limit() + Math.max(0, n - reader.freeSpace());

        ASequence newReader = reader.drop(readerDrop);
        ASequence newWriter = writer.drop(writerDrop).limit(writerLimit);

        return new CircularSequence(newReader, newWriter);
    }

    public ISequence expand(int n)
    {
        int readerExpand = Math.min(n, reader.freeSpace());
        int writerExpand = n - readerExpand;

        return new CircularSequence(reader.expand(readerExpand),
                                    writer.expand(writerExpand));
    }

    public Pair<ISequence, ISequence> write(ISequence seq) 
    {
        if(reader.freeSpace() > 0)
        {
            ArrayList<ISequence> seqs = new ArrayList<ISequence>();
            seqs.add(reader);
            seqs.add(writer);

            Pair<ISequence, Iterable<ISequence>> read = seq.read(seqs);
            Iterator<ISequence> iter = read.second.iterator();

            ASequence newReader= (ASequence)iter.next();
            ASequence newWriter= ((ASequence)iter.next()).limit(newReader.position());

            return new Pair<ISequence, ISequence>
                (new CircularSequence(newReader, newWriter),
                 read.first);
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
            ArrayList<ISequence> seqs = new ArrayList<ISequence>();
            seqs.add(reader);
            seqs.add(writer);

            Pair<ISequence, Iterable<ISequence>> writen = seq.write(seqs);
            Iterator<ISequence> iter = writen.second.iterator();

            ASequence newReader = (ASequence)iter.next();
            ASequence newWriter = ((ASequence)iter.next()).limit(newReader.position());

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