package madnet.event;

import java.util.Iterator;
import java.util.AbstractSet;
import java.util.HashSet;
import java.util.concurrent.LinkedBlockingQueue;

public class TriggerSet implements IEventSet {
    HashSet<Event> triggers;
    HashSet<Event> selections;
    LinkedBlockingQueue<Event> triggered;

    public TriggerSet() {
        triggers = new HashSet<Event>();
        selections = new HashSet<Event>();
        triggered = new LinkedBlockingQueue<Event>();
    }

    public static class Event implements IEvent {
        TriggerSet provider = null;

        @Override
        public void register(IEventSet provider) {
            ((TriggerSet)provider).triggers.add(this);
            this.provider = (TriggerSet)provider;
        }

        public void touch() {
            provider.triggered.add(this);
        }

        @Override
        public TriggerSet provider() {
            return provider;
        }
    }

    @Override
    public AbstractSet<Event> events() {
        return triggers;
    }

    @Override
    public Iterable<Event> selections() {
        final Iterator<Event> iterator = selections.iterator();

        return new Iterable<Event> () {
            @Override
            public Iterator<Event> iterator() {
                return new Iterator<Event>() {
                    @Override
                    public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    @Override
                    public Event next() {
                        Event next = iterator.next();
                        iterator.remove();

                        return next;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    @Override
    public void select() throws InterruptedException {
        if(selections.size() == 0)
            selections.add(triggered.take());

        while(triggered.size() > 0) {
            Event event = triggered.poll();

            if(event != null)
                selections.add(event);
        }
    }
}
