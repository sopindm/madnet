package madnet.range.nio;

import java.nio.Buffer;

public class Range extends madnet.range.Range {
    protected Buffer buffer;

    public Range(int begin, int end, Buffer buffer) throws Exception {
        super(begin, end);
        this.buffer = buffer.position(begin).limit(end);
    }

    public int begin() {
        return buffer.position();
    }

    public int end() {
        return buffer.limit();
    }

    protected Range begin(int n) {
        buffer.position(n);
        return this;
    }

    protected Range end(int n) {
        buffer.limit(n);
        return this;
    }

    public Buffer buffer() {
        return buffer;
    }
}
