package madnet.util;

public class Pair<First, Second>
{
    public final First first;
    public final Second second;

    public Pair(First first, Second second)
    {
        this.first = first;
        this.second = second;
    }

    public Pair<Second, First> flip() 
    {
        return new Pair<Second, First>(second, first);
    }
}