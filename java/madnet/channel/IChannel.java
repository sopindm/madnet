package madnet.channel;

public interface IChannel {
    public Result read(IChannel range) throws Exception;
    public Result write(IChannel range) throws Exception;
}
