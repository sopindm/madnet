package madnet.range;

public interface IRange extends Cloneable, madnet.channel.IChannel, Iterable
{
    public IRange clone() throws CloneNotSupportedException;

    public int begin();
    public int end();

    public int size(); 

    public IRange take(int n) throws Exception;
    public IRange drop(int n) throws Exception;

    public IRange takeLast(int n) throws Exception;
    public IRange dropLast(int n) throws Exception;

    public IRange expand(int n) throws Exception;
}
