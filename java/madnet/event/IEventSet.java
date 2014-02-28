package madnet.event;

public interface IEventSet extends java.io.Closeable {
    boolean isOpen();

    Iterable<? extends IEvent> events();
    Iterable<? extends IEvent> selections();

    boolean isEmpty();

    IEventSet push(IEvent event) throws Exception;
    void pop(IEvent event);
    
    IEventSet select() throws Exception;
    IEventSet selectIn(long milliseconds) throws Exception;
    IEventSet selectNow() throws Exception;

    void interrupt() throws Exception;
}
