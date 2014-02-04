package madnet.range.nio;

import madnet.range.IRange;
import java.nio.ByteBuffer;
import java.util.Iterator;

public class ByteRange extends Range implements Iterable<Byte> {
    public ByteRange(int begin, int end, ByteBuffer buffer) throws Exception {
        super(begin, end, buffer.duplicate());
    }

    public ByteBuffer buffer() {
        return (ByteBuffer)super.buffer();
    }

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
            int position = begin();

            public boolean hasNext() {
                return position < end();
            }

            public Byte next() {
                if(!hasNext())
                    throw new java.util.NoSuchElementException();

                position++;
                return buffer().get(position - 1);
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public ByteRange write(IRange range) throws Exception {
        if(!(range instanceof ByteRange))
            return null;

        ByteRange br = (ByteRange)range;

        if(br.size() > size()) {
            int size = size();

            buffer().put((ByteBuffer)br.buffer().duplicate().limit(br.begin() + size()));
            br.drop(size);
        }
        else {
            buffer().put(br.buffer());
            br.drop(br.size());
        }

        return this;
    }
}
