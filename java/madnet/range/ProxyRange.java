package madnet.range;

public class ProxyRange extends Range
{
    private Range range;

    public ProxyRange(Range range) {
        super(range.begin(), range.end());
        this.range = range;
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
    public ProxyRange write(madnet.channel.IChannel ch) throws Exception {
        if(this.range.write(ch) == null)
            return null;

        return this;
    }

    @Override
    public ProxyRange read(madnet.channel.IChannel ch) throws Exception {
        if(this.range.read(ch) == null)
            return null;

        return this;
    }
}
