package madnet.sequence.nio;

import madnet.sequence.IBuffer;
import madnet.sequence.ISequence;
import madnet.sequence.ASequence;

public class ByteBuffer implements IBuffer
{
    private int size;

    public ByteBuffer(int size)
    {
        this.size = size;
    }

    public int size() 
    {
        return this.size;
    }

    public ISequence sequence(int offset, int size)
    {
        return new ASequence(this, offset, size);
    }
}