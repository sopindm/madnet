package madnet.range.nio;

import madnet.range.IRange;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CharacterCodingException;
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

    public CharRange write(IRange range) throws Exception {
        if(!(range instanceof CharRange))
            return null;

        CharRange src = (CharRange)range;

        if(src.size() > size()) {
            int size = size();

            buffer().put((CharBuffer)src.buffer().duplicate().limit(size));
            src.drop(size);
        }
        else
            buffer().put(src.buffer());

        return this;
    }

    public CharRange writeBytes(ByteRange bytes, Charset charset) throws Exception {
        CharsetDecoder decoder = charset.newDecoder();
        CoderResult result = decoder.decode(bytes.buffer(), buffer(), true);

        if(result.isError())
            throw new CharacterCodingException();
        
        return this;
    }

    public CharRange readBytes(ByteRange bytes, Charset charset) throws Exception {
        CharsetEncoder encoder = charset.newEncoder();
        CoderResult result = encoder.encode(buffer(), bytes.buffer(), true);

        if(result.isError())
            throw new CharacterCodingException();
        
        return this;
    }
}
