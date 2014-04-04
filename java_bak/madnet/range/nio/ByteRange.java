package madnet.range.nio;

import madnet.channel.IChannel;
import madnet.channel.Result;
import java.nio.ByteBuffer;
import java.util.Iterator;

public class ByteRange extends Range {
    public ByteRange(int begin, int end, ByteBuffer buffer) throws Exception {
        super(begin, end, buffer.duplicate());
    }

    @Override
    public ByteBuffer buffer() {
        return (ByteBuffer)super.buffer();
    }

    @Override
    public ByteRange clone() throws CloneNotSupportedException {
        ByteRange r = (ByteRange)super.clone();
        r.buffer = buffer().duplicate();

        return r;
    }

    public byte get(int n) {
        return buffer().get(begin() + n);
    }

    @Override
    public Iterator<Byte> iterator() {
        return new Iterator<Byte>() {
            ByteBuffer buffer = buffer().duplicate();

            public boolean hasNext() {
                return buffer.position() != buffer.limit();
            }

            public Byte next() {
                if(!hasNext())
                    throw new java.util.NoSuchElementException();

                return buffer.get();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public boolean tryPush(Object o) throws Exception {
        if(!(o instanceof Byte))
            throw new IllegalArgumentException();

        if(size() == 0)
            return false;

        Byte b = (Byte)o;

        buffer().put(b);
        return true;
    }

    @Override
    public Object tryPop() throws Exception {
        if(size() == 0)
            return null;

        return buffer().get();
    }

    @Override
    public Result write(IChannel ch) throws Exception {
        if(!(ch instanceof ByteRange))
            return null;

        ByteRange br = (ByteRange)ch;

        int writeSize = Math.min(br.size(), size());

        if(writeSize < br.size()) {
            buffer().put((ByteBuffer)br.buffer().duplicate().limit(br.begin() + size()));
            br.drop(writeSize);
        }
        else 
            buffer().put(br.buffer());

        return new Result(writeSize, writeSize);
    }
}