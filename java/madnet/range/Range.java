package madnet.range;

import madnet.channel.IChannel;
import madnet.channel.Result;

abstract public class Range extends madnet.channel.Channel
    implements Iterable, Cloneable
{
    public Range(int begin, int end) {
        if(begin > end)
            throw new IllegalArgumentException();
    }

    public Range clone() throws CloneNotSupportedException {
        return (Range)super.clone();
    }

    abstract public int begin();
    abstract public int end();

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

    @Override
    public Result read(IChannel range) throws Exception {
        return null;
    }

    @Override
    public Result write(IChannel range) throws Exception {
        return null;
    }

    @Override
    public java.util.Iterator iterator() {
        throw new UnsupportedOperationException();
    }
}
