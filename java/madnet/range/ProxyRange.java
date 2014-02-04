package madnet.range;

public class ProxyRange extends Range
{
    private Range range;

    public ProxyRange(Range range) {
        super(range.begin(), range.end());
        this.range = range;
    }

    public int begin() {
        return range.begin();
    }

    protected ProxyRange begin(int n) {
        range.begin(n);
        return this;
    }

    public int end() {
        return range.end();
    }

    protected ProxyRange end(int n) {
        range.end(n);
        return this;
    }

    public ProxyRange clone() throws CloneNotSupportedException {
        ProxyRange pr=(ProxyRange)super.clone();
        pr.range = pr.range.clone();

        return pr;
    }

    public Range range() {
        return this.range;
    }

    public int size() {
        return range.size();
    }

    public ProxyRange take(int n) throws Exception {
        range.take(n);
        return this;
    }        

    public ProxyRange takeLast(int n) throws Exception {
        range.takeLast(n);
        return this;
    }        

    public ProxyRange drop(int n) throws Exception {
        range.drop(n);
        return this;
    }        

    public ProxyRange dropLast(int n) throws Exception {
        range.dropLast(n);
        return this;
    }        

    public ProxyRange expand(int n) throws Exception {
        range.expand(n);
        return this;
    }        

    public ProxyRange write(IRange range) throws Exception {
        if(this.range.write(range) == null)
            return null;

        return this;
    }

    public ProxyRange read(IRange range) throws Exception {
        if(this.range.read(range) == null)
            return null;

        return this;
    }
}
