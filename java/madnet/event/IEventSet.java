package madnet.event;

import java.util.AbstractSet;

public interface IEventSet extends java.io.Closeable {
    AbstractSet<? extends IEvent> events();
    AbstractSet<? extends IEvent> selections();

    void push(IEvent event);
    void pop(IEvent event);
    
    IEventSet select();
    IEventSet selectIn(long milliseconds);
    IEventSet selectNow();

    void interrupt();
}
