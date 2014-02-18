package madnet.channel;

public interface IChannel extends Cloneable {
    public IChannel clone() throws CloneNotSupportedException;

    public Result read(IChannel range) throws Exception;
    public Result write(IChannel range) throws Exception;
}
