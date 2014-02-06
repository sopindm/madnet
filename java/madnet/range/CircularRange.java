package madnet.range;

import java.util.ArrayList;
import madnet.channel.IChannel;

public class CircularRange extends Range
{
    private Range range;
    private Range limit;
    private int tail;

    public CircularRange(Range range, Range limit) throws Exception {
        super(range.begin(), range.end());
        this.range = range;
        this.tail = 0;
        this.limit = limit;

        if(range.begin() < limit.begin() || range.end() > limit.end())
            throw new IllegalArgumentException();
    }

    public Range first() {
        return range;
    }

    public CircularRange dropFirst() throws Exception {
        drop(range.size());
        return this;
    }

    public Range limit() {
        return limit;
    }

    @Override
    public int begin() {
        return range.begin();
    }

    @Override
    public int end() {
        if(range.end() < limit.end())
            return range.end();
        
        return limit.begin() + tail;
    }

    @Override
    protected CircularRange begin(int n) {
        if(n >= limit.end()) {
            range.begin(limit.begin() + n - limit.end());
            range.end(tail);

            tail = 0;
        }
        else
            range.begin(n);

        return this;
    }

    @Override
    protected CircularRange end(int n) {
        if(n < begin()) {
            tail = n - limit.begin();
            return this;
        }

        if(n >= limit.end()) {
            range.end(limit.end());
            tail = n - limit.end();
        }
        else
            range.end(n);

        return this;
    }

    @Override
    public int size() {
        return range.size() + tail;
    }

    @Override
    public CircularRange clone() throws CloneNotSupportedException {
        CircularRange range = (CircularRange)super.clone();
        range.range = this.range.clone();
        range.limit = this.limit.clone();
        return range;
    }

    @Override
    public CircularRange write(IChannel ch) throws Exception {
        if(this.range.write(ch) == null && ch.read(this.range) == null)
            return null;

        if(this.range.size() > 0 || size() == 0)
            return this;

        begin(this.range.begin());

        return write(ch);
    }

    @Override
    public CircularRange read(IChannel ch) throws Exception {
        if(this.range.read(ch) == null && ch.write(this.range) == null)
            return null;

        if(this.range.size() > 0 || size() == 0)
            return this;

        begin(this.range.begin());

        return read(ch); 
    }
}
