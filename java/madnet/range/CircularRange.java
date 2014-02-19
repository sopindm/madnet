package madnet.range;

import java.util.Iterator;
import madnet.channel.Result;
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

        begin(range.begin());

        if(range.begin() < limit.begin() || range.end() > limit.end())
            throw new IllegalArgumentException();
    }

    @Override
    public boolean readable() {
        return range.readable();
    }

    @Override
    public void closeRead() {
        range.closeRead();
    }

    @Override 
    public boolean writeable() {
        return range.writeable();
    }

    @Override
    public void closeWrite() {
        range.closeWrite();
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
        if(range.end() < limit.end() || tail == 0)
            return range.end();
        
        return limit.begin() + tail;
    }

    @Override
    protected CircularRange begin(int n) {
        if(n >= limit.end()) {
            range.begin(limit.begin() + n - limit.end());
            range.end(limit.begin() + tail);

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

        if(n > limit.end()) {
            range.end(limit.end());
            tail = n - limit.end();
        }
        else {
            range.end(n);
            tail = 0;
        }

        return this;
    }

    @Override
    public int size() {
        return range.size() + tail;
    }

    @Override
    public CircularRange expand(int n) {
        if(size() + n > limit.size())
            throw new IndexOutOfBoundsException();

        if(tail == 0)
            end(end() + n);
        else
            tail += n;

        return this;
    }

    @Override
    public CircularRange clone() throws CloneNotSupportedException {
        CircularRange range = (CircularRange)super.clone();
        range.range = this.range.clone();
        range.limit = this.limit.clone();
        return range;
    }

    @Override
    public Iterator iterator() {
        if(begin() <= end())
            return range.iterator();

        final Iterator firstIterator = range.iterator();
        final Iterator restIterator;
        try {
            restIterator = range.clone().begin(0).end(tail).iterator();
        }
        catch(CloneNotSupportedException e){
            throw new RuntimeException(e);
        }

        return new Iterator() {
            @Override
            public boolean hasNext() { 
                return firstIterator.hasNext() || restIterator.hasNext();
            }

            @Override
            public Object next() {
                if(firstIterator.hasNext())
                    return firstIterator.next();

                return restIterator.next();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    protected Result writeImpl(IChannel ch) throws Exception {
        Result writeResult = range.write(ch);

        if(writeResult == null) {
            writeResult = ch.read(this.range);

            if(writeResult == null)
                return null;
        }

        if(range.size() > 0 || size() == 0) {
            begin(range.begin());
            return writeResult;
        }

        begin(range.begin());
        return writeResult.add(write(ch));
    }

    @Override
    protected Result readImpl(IChannel ch) throws Exception {
        Result readResult = range.read(ch);

        if(readResult == null) {
            readResult = ch.write(this.range);

            if(readResult == null)
                return null;
        }

        if(size() == 0 || range.size() > 0) {
            begin(range.begin());
            return readResult;
        }

        begin(range.begin());
        return readResult.add(read(ch));
    }
}
