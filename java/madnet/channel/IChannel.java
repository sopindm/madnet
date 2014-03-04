package madnet.channel;

public interface IChannel extends Cloneable {
    boolean readable();
    boolean writeable();

    void closeRead();
    void closeWrite();

    public Events events();
    void register(madnet.event.ISignalSet set);

    public IChannel clone() throws CloneNotSupportedException;

    public Result read(IChannel range) throws Exception;
    public Result write(IChannel range) throws Exception;
}
