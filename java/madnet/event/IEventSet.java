package madnet.event;

import java.util.AbstractSet;

public interface IEventSet {
    //void cancel();

    AbstractSet<? extends IEvent> events();
    AbstractSet<? extends IEvent> selections();

    void push(IEvent event);
    
    IEventSet select();
    IEventSet selectIn(long milliseconds);
    IEventSet selectNow();

    void interrupt();
}
