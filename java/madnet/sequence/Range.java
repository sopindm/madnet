package madnet.sequence;

public class Range implements IRange
{
    private int begin;
    private int end;

    protected Range(int begin, int end, boolean checkBounds) {
        this.begin = begin;
        this.end = end;

        if(checkBounds && begin > end)
            throw new IllegalArgumentException();
    }

    public Range(int begin, int end) {
        this(begin, end, true);
    }

    public int begin() { return begin; };
    public int end() { return end; };

    public Range begin(int value) {
        this.begin = value;
        return this;
    }

    public Range end(int value) {
        this.end = value;
        return this;
    }

    public boolean equals(Object o) {
        if(!(o instanceof Range))
            return false;

        Range r = (Range)o;
        return r.begin() == begin() && r.end() == end();
    }

    public int hashCode() {
        int hash = 153;

        hash = hash * 31 + begin();
        hash = hash * 31 + end();

        return hash;
    } 

    public Range clone() throws CloneNotSupportedException {
        return (Range)super.clone();
    }

    public Integer size() {
        return end() - begin();
    }

    public Range take(int n) throws Exception {
        if(n > size())
            throw new IndexOutOfBoundsException();

        return end(begin() + n);
    }

    public Range drop(int n) throws Exception {
        if(n > size())
            throw new IndexOutOfBoundsException();

        return begin(begin() + n);
    }

    public Range takeLast(int n) throws Exception {
        if(n > size())
            throw new IndexOutOfBoundsException();

        return begin(end() - n);
    }

    public Range dropLast(int n) throws Exception {
        if(n > size())
            throw new IndexOutOfBoundsException();

        return end(end() - n);
    }

    public Range expand(int n) throws Exception {
        return end(end() + n);
    }

    public Range split(int n) throws Exception {
        Range taken = clone().take(n);
        drop(n);

        return taken;
    }
}
