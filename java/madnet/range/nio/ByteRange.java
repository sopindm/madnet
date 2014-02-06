package madnet.range.nio;

import madnet.channel.IChannel;
import madnet.range.IRange;
import java.nio.ByteBuffer;
import java.util.Iterator;

public class ByteRange extends Range implements Iterable<Byte> {
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
    public ByteRange write(IChannel ch) throws Exception {
        if(!(ch instanceof ByteRange))
            return null;

        ByteRange br = (ByteRange)ch;

        if(br.size() > size()) {
            int size = size();

            buffer().put((ByteBuffer)br.buffer().duplicate().limit(br.begin() + size()));
            br.drop(size);
        }
        else 
            buffer().put(br.buffer());

        return this;
    }
}
