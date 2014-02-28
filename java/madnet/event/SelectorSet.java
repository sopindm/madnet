package madnet.event;

import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.channels.SelectableChannel;
import java.util.Iterator;
import java.util.Set;

public class SelectorSet implements IEventSet {
    Selector selector;

    EventIterable events;
    EventIterable selections;

    public SelectorSet() throws Exception {
        selector = Selector.open();

        events = new EventIterable(selector.keys());
        selections = new EventIterable(selector.selectedKeys());
    }

    @Override
    public boolean isEmpty() {
        return selector.keys().size() == 0;
    }

    @Override
    public boolean isOpen() {
        return selector.isOpen();
    }

    @Override
    public void close() throws java.io.IOException {
        for(IEvent e : events())
            ((Event)e).provider = null;

        selector.close();
    }

    private static class EventIterable implements Iterable<IEvent> {
        Set<SelectionKey> source;

        public EventIterable(Set<SelectionKey> source) {
            this.source = source;
        }

        @Override
        public Iterator<IEvent> iterator() {
            final Iterator<SelectionKey> iterator = source.iterator();

            return new Iterator<IEvent>() {
                @Override
                public boolean hasNext() { return iterator.hasNext(); }

                @Override
                public IEvent next() {
                    return (IEvent)iterator.next().attachment();
                }

                @Override
                public void remove() { iterator.remove(); }
            };
        }
    }

    @Override
    public Iterable<IEvent> events() { return events; };

    @Override
    public Iterable<IEvent> selections() { return selections; };

    public static class Event implements IEvent {
        SelectableChannel channel = null;
        int op;
        SelectionKey key = null;

        SelectorSet provider = null;

        @Override
        public SelectorSet provider() { return provider; }

        public Event(SelectableChannel channel, int op) {
            this.channel = channel;
            this.op = op;
        }

        @Override
        public void register(IEventSet set) throws Exception {
            if(provider != null)
                throw new IllegalArgumentException();

            provider = (SelectorSet)set.push(this);

            if(provider == null)
                throw new IllegalArgumentException();
        }

        private void register(Selector selector) throws Exception {
            key = channel.register(selector, 0);
            key.attach(this);
        }

        @Override
        public void close() {
            attachment = null;
            cancel();
        }

        @Override
        public void cancel() {
            key.cancel();
            provider = null;
        }

        @Override
        public void start() {
            key.interestOps(op);
        }

        @Override
        public void stop() {
            key.interestOps(0);
        }

        Object attachment;

        @Override
        public void attach(Object attachment) {
            this.attachment = attachment;
        }

        @Override
        public Object attachment() { 
            return attachment;
        }
    }

    @Override
    public SelectorSet push(IEvent event) throws Exception {
        if(!(event instanceof Event))
            throw new IllegalArgumentException();

        ((Event)event).register(selector);
        return this;
    }

    @Override
    public void pop(IEvent event) {
    }

    @Override
    public SelectorSet select() throws Exception {
        selector.select();
        return this;
    }

    @Override
    public SelectorSet selectIn(long milliseconds) throws Exception {
        selector.select(milliseconds);
        return this;
    }

    @Override
    public SelectorSet selectNow() throws Exception {
        selector.selectNow();
        return this;
    }

    @Override
    public void interrupt() throws Exception {
        selector.wakeup();
        selectNow();
    }
}
