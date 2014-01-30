package madnet.range;

import java.util.ArrayList;

public class CircularRange extends Range
{
    private Range limit;

    public CircularRange(int begin, int end, Range limit) throws Exception {
        super(begin, end, false);
        this.limit = limit.clone();

        if(begin < limit.begin() || end > limit.end())
            throw new IllegalArgumentException();
    }

    public Range limit() throws Exception {
        return limit.clone();
    }

    private void applyLimit() {
        while(begin() > limit.end())
            begin(begin() - limit.size());

        while(end() > limit.end())
            end(end() - limit.size());
    }

    public Integer size() {
        if(begin() <= end())
            return super.size();

        return (limit.end() - begin()) + (end() - limit.begin());
    }

    public CircularRange clone() throws CloneNotSupportedException {
        CircularRange range = (CircularRange)super.clone();
        range.limit = limit.clone();
        return range;
    }

    public CircularRange take(int n) throws Exception {
        super.take(n);
        applyLimit();

        return this;
    }

    public CircularRange takeLast(int n) throws Exception {
        super.takeLast(n);
        applyLimit();

        return this;
    }

    public CircularRange drop(int n) throws Exception {
        super.drop(n);
        applyLimit();

        return this;
    }

    public CircularRange dropLast(int n) throws Exception {
        super.dropLast(n);
        applyLimit();

        return this;
    }

    public CircularRange expand(int n) throws Exception {
        super.expand(n);
        applyLimit();

        return this;
    }

    public Iterable<Range> ranges() throws Exception {
        ArrayList<Range> rangesList = new ArrayList<Range>();

        if(begin() <= end()) {
            rangesList.add(clone());
            return rangesList;
        }
            
        rangesList.add(clone().take(limit.end() - begin()));
        rangesList.add(clone().takeLast(end() - limit.begin()));
        return rangesList;
    }
}
