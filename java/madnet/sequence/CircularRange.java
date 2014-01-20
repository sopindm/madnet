package madnet.sequence;

public abstract class CircularRange extends Range
{
    private Range limit;
    public CircularRange(Range limit) throws Exception {
        this.limit = limit.clone();
    }

    public Range limit() throws Exception {
        return limit.clone();
    }

    public CircularRange clone() throws CloneNotSupportedException {
        CircularRange range = (CircularRange)super.clone();
        range.limit = limit.clone();
        return range;
    }
}