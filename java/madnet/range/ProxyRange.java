package madnet.range;

import madnet.channel.Result;
import java.util.Iterator;

public class ProxyRange extends Range
{
    private Range range;

    public ProxyRange(Range range) {
        this.range = range;
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

    @Override
    public int begin() {
        return range.begin();
    }

    @Override
    protected ProxyRange begin(int n) {
        range.begin(n);
        return this;
    }

    @Override
    public int end() {
        return range.end();
    }

    @Override
    protected ProxyRange end(int n) {
        range.end(n);
        return this;
    }

    @Override
    public ProxyRange clone() throws CloneNotSupportedException {
        ProxyRange pr=(ProxyRange)super.clone();
        pr.range = pr.range.clone();

        return pr;
    }

    @Override
    public Iterator iterator() {
        return range.iterator();
    }

    public Range range() {
        return this.range;
    }

    @Override
    public int size() {
        return range.size();
    }

    @Override
    public ProxyRange take(int n) throws Exception {
        range.take(n);
        return this;
    }        

    @Override
    public ProxyRange takeLast(int n) throws Exception {
        range.takeLast(n);
        return this;
    }        

    @Override
    public ProxyRange drop(int n) throws Exception {
        range.drop(n);
        return this;
    }        

    @Override
    public ProxyRange dropLast(int n) throws Exception {
        range.dropLast(n);
        return this;
    }        

    @Override
    public ProxyRange expand(int n) throws Exception {
        range.expand(n);
        return this;
    }        

    @Override
    protected Result writeImpl(madnet.channel.IChannel ch) throws Exception {
        Result writeResult = range.write(ch);
        if(writeResult != null)
            return writeResult;

        return ch.read(range);
    }

    @Override
    protected Result readImpl(madnet.channel.IChannel ch) throws Exception {
        Result readResult = range.read(ch);
        if(readResult != null)
            return readResult;

        return ch.write(range);
    }
}
