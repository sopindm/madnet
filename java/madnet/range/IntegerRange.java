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

    @Override
    public int begin() {
        return begin;
    }

    @Override
    public int end() {
        return end;
    }

    @Override
    protected IntegerRange begin(int n) {
        this.begin = n;

        if(this.begin > this.end)
            throw new IllegalArgumentException();

        return this;
    }

    @Override
    protected IntegerRange end(int n) {
        this.end = n;

        if(this.begin > this.end)
            throw new IllegalArgumentException();

        return this;
    }

    @Override
    public int size() {
        return end() - begin();
    }
}
