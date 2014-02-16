package madnet.range;

import madnet.channel.IChannel;
import madnet.channel.Result;
import java.util.ArrayList;
import java.util.Iterator;

public class ObjectRange extends IntegerRange {
    private ArrayList<Object> buffer;

    public ObjectRange(int begin, int end, ArrayList<Object> buffer) throws Exception {
        super(begin, end);

        this.buffer = buffer;

        if(begin < 0 || end > buffer.size())
            throw new IllegalArgumentException();
    }

    @Override 
    public ObjectRange expand(int n) throws Exception {
        if(end() + n > buffer.size())
            throw new IllegalArgumentException();

        super.expand(n);
        return this;
    }

    @Override
    public Iterator iterator() {
        return new Iterator<Object>() {
            private int position = begin();

            @Override
            public boolean hasNext() { return position != end(); };
            
            @Override
            public Object next() { 
                if(!hasNext())
                    throw new java.util.NoSuchElementException();

                position++;
                return buffer.get(position - 1);
            };

            @Override
            public void remove() { throw new UnsupportedOperationException(); }
        };
    }

    public Object get(int index) {
        if(index < 0 || index > end() - begin())
            throw new IllegalArgumentException();

        return buffer.get(begin() + index);
    }

    public void set(int index, Object value) {
        if(index < 0 || index > end() - begin())
            throw new IllegalArgumentException();

        buffer.set(begin() + index, value);
    }

    @Override
    public Result write(IChannel channel) throws Exception {
        if(!(channel instanceof Range))
            return null;
        
        Range r = (Range)channel;

        int writen = 0;
        Iterator reader = r.iterator();

        while(writen < end() - begin() && reader.hasNext()) {
            set(writen, reader.next());
            writen++;
        }

        drop(writen);
        r.drop(writen);

        return new Result(writen, writen);
    }
}
