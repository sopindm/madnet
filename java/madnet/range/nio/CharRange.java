package madnet.range.nio;

import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CharacterCodingException;
import java.util.Iterator;
import madnet.channel.Result;

public class CharRange extends Range {
    public CharRange(int begin, int end, CharBuffer buffer) throws Exception {
        super(begin, end, buffer.duplicate());
    }

    @Override
    public CharBuffer buffer() {
        return (CharBuffer)super.buffer();
    }

    @Override
    public CharRange clone() throws CloneNotSupportedException {
        CharRange r = (CharRange)super.clone();
        r.buffer = buffer().duplicate();

        return r;
    }

    public char get(int i) {
        return buffer().get(begin() + i);
    } 

    @Override
    public Iterator<Character> iterator() {
        return new Iterator<Character>() {
            CharBuffer buffer = buffer().duplicate();

            public boolean hasNext() {
                return buffer.position() != buffer.limit();
            }

            public Character next() {
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
    public Result write(madnet.channel.IChannel ch) throws Exception {
        if(!(ch instanceof CharRange))
            return null;

        CharRange src = (CharRange)ch;

        int writeSize = Math.min(size(), src.size());

        if(writeSize < src.size()) {
            buffer().put((CharBuffer)src.buffer().duplicate().limit(writeSize));
            src.drop(writeSize);
        }
        else
            buffer().put(src.buffer());

        return new Result(writeSize, writeSize);
    }

    public Result writeBytes(ByteRange bytes, Charset charset) throws Exception {
        CharsetDecoder decoder = charset.newDecoder();

        int charPosition = begin();
        int bytePosition = bytes.begin();

        CoderResult result = decoder.decode(bytes.buffer(), buffer(), true);

        if(result.isError())
            throw new CharacterCodingException();

        return new Result(bytePosition - bytes.begin(),
                          begin() - charPosition);
    }

    public Result readBytes(ByteRange bytes, Charset charset) throws Exception {
        CharsetEncoder encoder = charset.newEncoder();

        int charPosition = begin();
        int bytePosition = bytes.begin();

        CoderResult result = encoder.encode(buffer(), bytes.buffer(), true);

        if(result.isError())
            throw new CharacterCodingException();

        return new Result(begin() - charPosition,
                          bytePosition - bytes.begin());
    }
}
