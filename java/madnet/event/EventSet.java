package madnet.event;

import java.util.AbstractSet;
import java.util.HashSet;

public abstract class EventSet<Event extends madnet.event.Event> implements IEventSet {
    HashSet<Event> events = new HashSet<Event>();
    HashSet<Event> selections = new HashSet<Event>();

    @Override
    public AbstractSet<Event> events() {
        return events;
    }

    @Override
    public AbstractSet<Event> selections() {
        return selections;
    }
}
