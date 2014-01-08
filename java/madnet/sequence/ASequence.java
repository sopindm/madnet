package madnet.sequence;

public class ASequence implements ISequence
{
    private final IBuffer _buffer;
    private final long _position;
    private final long _size;

    public ASequence(IBuffer buffer, long position, long size)
    {
        _buffer = buffer;
        _position = position;
        _size = size;
    }

    public IBuffer buffer()
    {
        return _buffer;
    }

    public long position()
    {
        return _position;
    }

    public long size()
    {
        return _size;
    }

    public ISequence take(long n)
    {
        return _buffer.sequence(_position, n);
    }

    public ISequence drop(long n)
    {
        return _buffer.sequence(_position + n, _size - n);
    }

    public ISequence expand(long n)
    {
        return _buffer.sequence(_position, _size + n);
    }
}

