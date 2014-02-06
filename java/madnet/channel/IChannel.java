package madnet.channel;

public interface IChannel {
    public IChannel read(IChannel range) throws Exception;
    public IChannel write(IChannel range) throws Exception;
}
