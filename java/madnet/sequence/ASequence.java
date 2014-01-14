package madnet.sequence;

import java.util.ArrayList;
import madnet.util.Pair;

public class ASequence extends ISequence
{
    private final IBuffer buffer;
    private final int position;
    private final int size;

    public ASequence(IBuffer buffer, int position, int size)
    {
        this.buffer = buffer;
        this.position = position;
        this.size = size;
    }

    public IBuffer buffer()
    {
        return buffer;
    }

    public int position()
    {
        return position;
    }

    public int size()
    {
        return size;
    }

    public int freeSpace() 
    {
        return buffer().size() - position() - size();
    }

    public ASequence take(int n)
    {
        return (ASequence)buffer.sequence(position, n);
    }

    public ASequence drop(int n)
    {
        return (ASequence)buffer.sequence(position + n, size - n);
    }

    public ASequence expand(int n)
    {
        return (ASequence)buffer.sequence(position, size + n);
    }

    public Pair<ISequence, ISequence> read(ISequence seq)
    {
        Pair<ISequence, ISequence> read = readImpl(seq);

        if(read != null)
            return read;

        read = seq.writeImpl(this);
        if(read != null)
            return new Pair<ISequence, ISequence>(read.second, read.first);

        throw new UnsupportedOperationException();
    }

    public Pair<ISequence, Iterable<ISequence>> read(Iterable<ISequence> seqs)
    {
        ArrayList<ISequence> ret = new ArrayList<ISequence>();

        ISequence thisSeq = this;

        boolean readSuccess = true;
        for(ISequence seq : seqs) 
        {
            if(!readSuccess)
            {
                ret.add(seq);
                continue;
            }

            Pair<ISequence, ISequence> read = thisSeq.read(seq);
            thisSeq = read.first;

            if(read.second.freeSpace() > 0)
                readSuccess = false;

            ret.add(read.second);
        }

        return new Pair<ISequence, Iterable<ISequence>>(thisSeq, ret);
    }

    public Pair<ISequence, ISequence> write(ISequence seq)
    {
        Pair<ISequence, ISequence> writen = writeImpl(seq);

        if(writen != null)
            return writen;

        writen = seq.readImpl(this);
        if(writen != null)
            return new Pair<ISequence, ISequence>(writen.second, writen.first);

        throw new UnsupportedOperationException();
    }

    public Pair<ISequence, Iterable<ISequence>> write(Iterable<ISequence> seqs)
    {
        ArrayList<ISequence> ret = new ArrayList<ISequence>();

        ISequence thisSeq = this;

        boolean writeSuccess = true;
        for(ISequence seq : seqs) 
        {
            if(!writeSuccess)
            {
                ret.add(seq);
                continue;
            }

            Pair<ISequence, ISequence> writen = thisSeq.write(seq);
            thisSeq = writen.first;

            if(writen.second.size() > 0)
                writeSuccess = false;

            ret.add(writen.second);
        }

        return new Pair<ISequence, Iterable<ISequence>>(thisSeq, ret);
    }

    protected Pair<ISequence, ISequence> readImpl(ISequence seq)
    {
        return null;
    }

    protected Pair<ISequence, ISequence> writeImpl(ISequence seq)
    {
        return null;
    }
}

