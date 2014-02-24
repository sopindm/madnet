package madnet.event;

import java.util.AbstractSet;
import java.util.HashSet;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class EventSet<E extends madnet.event.Event> implements IEventSet {
    HashSet<E> events = new HashSet<E>();
    HashSet<E> selections = new HashSet<E>();
    protected ConcurrentLinkedQueue<E> canceling = new ConcurrentLinkedQueue<E>();

    boolean closed = false;

    protected boolean isClosed() { return closed; }

    @Override
    public void close() {
        events.clear();
        selections.clear();
        canceling.clear();

        closed = true;
    }

    @Override
    public AbstractSet<E> events() {
        return events;
    }

    @Override
    public AbstractSet<E> selections() {
        return selections;
    }
}
