package madnet.range;

public class IntegerRange extends Range
{
    private int begin;
    private int end;

    public IntegerRange(int begin, int end) {
        super(begin, end);
        
        this.begin = begin;
        this.end = end;
    }

    public int begin() {
        return begin;
    }

    public int end() {
        return end;
    }

    protected IntegerRange begin(int n) {
        this.begin = n;

        if(this.begin > this.end)
            throw new IllegalArgumentException();

        return this;
    }

    protected IntegerRange end(int n) {
        this.end = n;

        if(this.begin > this.end)
            throw new IllegalArgumentException();

        return this;
    }

    public int size() {
        return end() - begin();
    }
}
