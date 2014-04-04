package madnet.channel;

public interface IChannel extends IQueue, Cloneable, java.nio.channels.Channel {
    public IEvents events();
    void register(madnet.event.ISignalSet set) throws Exception;

    public IChannel clone() throws CloneNotSupportedException;

    public Result read(IChannel range) throws Exception;
    public Result write(IChannel range) throws Exception;
}
