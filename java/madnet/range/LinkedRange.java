package madnet.range;

public class LinkedRange extends ProxyRange
{
    private IRange prev;
    private IRange next;

    public LinkedRange(Range range, IRange prev, IRange next) {
        super(range);

        this.prev = prev;
        this.next = next;
    }

    public IRange next() {
        return this.next;
    }

    public IRange prev() {
        return this.prev; 
    }

    public LinkedRange drop(int n) throws Exception {
        super.drop(n);
        if(prev != null)
            prev.expand(n);

        return this;
    }

    public LinkedRange expand(int n) throws Exception {
        super.expand(n);
        if(next != null)
            next.drop(n);

        return this;
    }
}
