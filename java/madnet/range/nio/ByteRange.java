package madnet.range.nio;

import java.nio.ByteBuffer;
import java.util.Iterator;

public class ByteRange extends Range implements Iterable<Byte> {
    public ByteRange(int begin, int end, ByteBuffer buffer) {
        super(begin, end, buffer.duplicate());
    }

    public ByteBuffer buffer() {
        return (ByteBuffer)super.buffer();
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
}
