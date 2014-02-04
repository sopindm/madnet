package madnet.range;

public interface IRange extends Cloneable
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

    public IRange read(IRange range) throws Exception;
    public IRange write(IRange range) throws Exception;
}
