package madnet.range.nio;

import java.nio.Buffer;

public class Range extends madnet.range.Range {
    private int begin;
    private int end;

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
