package madnet.event;

public interface IEvent extends java.io.Closeable {
    void register(IEventSet set);
    void cancel();

    //boolean persistent();

    //void activate();
    //void deactivate();

    IEventSet provider();

    void attach(Object attachment);
    Object attachment();
}
