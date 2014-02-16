package madnet.range;

import madnet.channel.Result;
import madnet.channel.IChannel;

public class LinkedRange extends ProxyRange
{
    private Range prev;
    private Range next;

    public LinkedRange(Range range, Range prev, Range next) {
        super(range);

        this.prev = prev;
        this.next = next;
    }

    public Range next() {
        return this.next;
    }

    public Range prev() {
        return this.prev; 
    }

    @Override
    public LinkedRange drop(int n) throws Exception {
        super.drop(n);
        if(prev != null)
            prev.expand(n);

        return this;
    }

    @Override
    public LinkedRange expand(int n) throws Exception {
        super.expand(n);
        if(next != null)
            next.drop(n);

        return this;
    }

    @Override
    public Result write(IChannel ch) throws Exception {
        Result result = super.write(ch);
        if(result == null)
            return result;

        if(prev != null)
            prev.expand(result.writen);

        return result;
    }

    @Override
    public Result read(IChannel ch) throws Exception {
        Result result = super.read(ch);
        if(result == null)
            return result;

        if(prev != null)
            prev.expand(result.read);

        return result;
    }
}
