package madnet.range.nio;

import java.nio.Buffer;

public class Range extends madnet.range.Range {
    private Buffer buffer;

    public Range(int begin, int end, Buffer buffer) {
        super(begin, end);

        this.buffer = buffer.position(begin).limit(end);
    }

    protected Range syncToBuffer() {
        begin(buffer.position());
        end(buffer.limit());

        return this;
    }

    public Range begin(int n) {
        super.begin(n);
        buffer.position(n);

        return this;
    }

    public Range end(int n) {
        super.end(n);
        buffer.limit(n);
        
        return this;
    }

    public Buffer buffer() {
        return buffer;
    }
}
