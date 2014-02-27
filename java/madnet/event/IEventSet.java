package madnet.event;

public interface IEventSet extends java.io.Closeable {
    Iterable<? extends IEvent> events();
    Iterable<? extends IEvent> selections();

    IEventSet push(IEvent event) throws Exception;
    void pop(IEvent event);
    
    IEventSet select() throws Exception;
    IEventSet selectIn(long milliseconds) throws Exception;
    IEventSet selectNow() throws Exception;

    void interrupt();
}
