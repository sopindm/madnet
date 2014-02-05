package madnet.range.nio;

import madnet.range.IRange;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.util.Iterator;

public class CharRange extends Range implements Iterable<Character> {
    public CharRange(int begin, int end, CharBuffer buffer) throws Exception {
        super(begin, end, buffer.duplicate());
    }

    public CharBuffer buffer() {
        return (CharBuffer)super.buffer();
    }

    public CharRange clone() throws CloneNotSupportedException {
        CharRange r = (CharRange)super.clone();
        r.buffer = buffer().duplicate();

        return r;
    }

    public char get(int i) {
        return buffer().get(begin() + i);
    } 

    public Iterator<Character> iterator() {
        return new Iterator<Character>() {
            int position = begin();

            public boolean hasNext() {
                return position < end();
            }

            public Character next() {
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

    public CharRange writeBytes(ByteRange bytes, Charset charset) {
        CharsetDecoder decoder = charset.newDecoder();
        decoder.decode(bytes.buffer(), buffer(), true);

        return this;
    }
}
