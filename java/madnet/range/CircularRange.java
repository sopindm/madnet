package madnet.range;

import java.util.ArrayList;

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

    public int begin() {
        return range.begin();
    }

    public int end() {
        if(range.end() < limit.end())
            return range.end();
        
        return limit.begin() + tail;
    }

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

    public int size() {
        return range.size() + tail;
    }

    public CircularRange clone() throws CloneNotSupportedException {
        CircularRange range = (CircularRange)super.clone();
        range.range = this.range.clone();
        range.limit = this.limit.clone();
        return range;
    }

    public CircularRange write(IRange range) throws Exception {
        if(this.range.write(range) == null && range.read(this.range) == null)
            return null;

        if(this.range.size() > 0 || range.size() == 0 || size() == 0)
            return this;

        begin(this.range.begin());

        return write(range);
    }

    public CircularRange read(IRange range) throws Exception {
        if(this.range.read(range) == null && range.write(this.range) == null)
            return null;

        if(this.range.size() > 0 || range.size() == 0 || size() == 0)
            return this;

        begin(this.range.begin());

        return read(range); 
    }
}
