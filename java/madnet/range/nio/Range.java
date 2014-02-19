package madnet.range.nio;

import java.nio.Buffer;

public class Range extends madnet.range.Range {
    private int begin;
    private int end;

    private boolean readable = true;
    private boolean writeable = true;

    @Override
    public boolean readable() {
        return readable;
    }

    @Override
    public void closeRead() {
        readable = false;
    }

    @Override
    public boolean writeable() {
        return writeable;
    }

    @Override
    public void closeWrite() {
        writeable = false;
    }

    protected Buffer buffer;

    public Range(int begin, int end, Buffer buffer) throws Exception {
        super(begin, end);
        this.buffer = buffer.position(begin).limit(end);
    }

    @Override
    public int begin() {
        return buffer.position();
    }

    @Override
    public int end() {
        return buffer.limit();
    }

    @Override
    protected Range begin(int n) {
        buffer.position(n);
        return this;
    }

    @Override
    protected Range end(int n) {
        buffer.limit(n);
        return this;
    }

    public Buffer buffer() {
        return buffer;
    }
}
