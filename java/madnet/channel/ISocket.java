package madnet.channel;

public interface ISocket extends java.io.Closeable {
    IChannel reader() throws Exception;
    IChannel writer() throws Exception;
}
