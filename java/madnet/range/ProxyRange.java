package madnet.range;

public class ProxyRange implements IRange
{
    private IRange range;

    public ProxyRange(IRange range) {
        this.range = range;
    }

    public boolean equals(Object o) {
        if(!(o instanceof ProxyRange))
            return false;

        return ((ProxyRange)o).range.equals(range);
    }

    public int hashCode() {
        return 13891 + range.hashCode() * 31;
    }

    public ProxyRange clone() throws CloneNotSupportedException {
        ProxyRange pr=(ProxyRange)super.clone();
        pr.range = pr.range.clone();

        return pr;
    }

    public IRange range() {
        return this.range;
    }

    public Integer size() {
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
        this.range.write(range);
        return this;
    }

    public ProxyRange read(IRange range) throws Exception {
        this.range.read(range);
        return this;
    }
}
