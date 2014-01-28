package madnet.sequence;

public class LinkedRange extends ProxyRange
{
    private IRange prev;
    private IRange next;

    public LinkedRange(IRange range, IRange prev, IRange next) {
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

    public boolean equals(Object o) {
        if(!(o instanceof LinkedRange))
            return false;

        LinkedRange r = (LinkedRange)o;

        return super.equals(r) &&
            this.next == r.next &&
            this.prev == r.prev;
    }

    public int hashCode() {
        int hash = 31 * super.hashCode();
        hash = 31 * hash + System.identityHashCode(prev);
        hash = 31 * hash + System.identityHashCode(next);

        return hash;
    }
}
