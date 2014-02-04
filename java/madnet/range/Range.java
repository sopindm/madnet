package madnet.range;

abstract public class Range implements IRange
{
    public Range(int begin, int end) {
        if(begin > end)
            throw new IllegalArgumentException();
    }

    public Range clone() throws CloneNotSupportedException {
        return (Range)super.clone();
    }

    abstract protected Range begin(int n);
    abstract protected Range end(int n);

    public int size() {
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
