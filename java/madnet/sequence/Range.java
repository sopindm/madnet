package madnet.sequence;

public abstract class Range implements IRange
{
    abstract public int begin();
    abstract public Range begin(int value);

    abstract public int end();
    abstract public Range end(int value);

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