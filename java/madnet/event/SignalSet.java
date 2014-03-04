package madnet.event;

import java.util.AbstractSet;
import java.util.HashSet;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class SignalSet<E extends madnet.event.Signal> implements ISignalSet {
    HashSet<E> signals = new HashSet<E>();
    HashSet<E> selections = new HashSet<E>();
    protected ConcurrentLinkedQueue<E> canceling = new ConcurrentLinkedQueue<E>();

    boolean closed = false;

    public boolean isEmpty() {
        return signals.size() == 0;
    }

    @Override
    public boolean isOpen() { return !closed; };

    @Override
    public void close() {
        signals.clear();
        selections.clear();
        canceling.clear();

        closed = true;
    }

    @Override
    public AbstractSet<E> signals() {
        return signals;
    }

    @Override
    public AbstractSet<E> selections() {
        return selections;
    }
}
