package madnet.range;

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

    public IRange read(IRange range) throws Exception {
        return null;
    }

    public IRange write(IRange range) throws Exception {
        return null;
    }
}