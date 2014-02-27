package madnet.event;

import java.util.Set;

public interface IEventSet extends java.io.Closeable {
    Iterable events();
    Iterable selections();

    void push(IEvent event) throws Exception;
    void pop(IEvent event);
    
    IEventSet select() throws Exception;
    IEventSet selectIn(long milliseconds) throws Exception;
    IEventSet selectNow() throws Exception;

    void interrupt();
}
