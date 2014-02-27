package madnet.event;

public interface IEvent extends java.io.Closeable {
    void register(IEventSet set) throws Exception;
    void cancel() throws Exception;

    void start();
    void stop();

    IEventSet provider();

    void attach(Object attachment);
    Object attachment();
}
