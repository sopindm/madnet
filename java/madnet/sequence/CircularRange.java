package madnet.sequence;

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

    public boolean equals(Object o) {
        if(!(o instanceof CircularRange))
            return false;

        CircularRange cr = (CircularRange)o;

        return super.equals(cr) && limit.equals(cr.limit);
    }

    public int hashCode() {
        return super.hashCode() * 31 + limit.hashCode();
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

    public Iterable<Range> ranges() {
        ArrayList<Range> rangesList = new ArrayList<Range>();

        if(begin() < end()) {
            rangesList.add(new Range(begin(), end()));
            return rangesList;
        }
            
        rangesList.add(new Range(begin(), limit.end()));
        rangesList.add(new Range(limit.begin(), end()));
        return rangesList;
    }
}
