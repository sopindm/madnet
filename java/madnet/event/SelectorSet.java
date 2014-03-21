package madnet.event;

import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.channels.SelectableChannel;
import java.util.Iterator;
import java.util.Set;

public class SelectorSet implements ISignalSet {
    Selector selector;

    SignalIterable signals;
    SignalIterable selections;

    public SelectorSet() throws Exception {
        selector = Selector.open();

        signals = new SignalIterable(selector.keys());
        selections = new SignalIterable(selector.selectedKeys());
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
        for(ISignal e : signals())
            ((Signal)e).provider = null;

        selector.close();
    }

    private static class SignalIterable implements Iterable<ISignal> {
        Set<SelectionKey> source;

        public SignalIterable(Set<SelectionKey> source) {
            this.source = source;
        }

        @Override
        public Iterator<ISignal> iterator() {
            final Iterator<SelectionKey> iterator = source.iterator();

            return new Iterator<ISignal>() {
                @Override
                public boolean hasNext() { return iterator.hasNext(); }

                @Override
                public ISignal next() {
                    return (ISignal)iterator.next().attachment();
                }

                @Override
                public void remove() { iterator.remove(); }
            };
        }
    }

    @Override
    public Iterable<ISignal> signals() { return signals; };

    @Override
    public Iterable<ISignal> selections() { return selections; };

    public static class Signal extends Event implements ISignal {
        SelectableChannel channel = null;
        int op;
        SelectionKey key = null;

        SelectorSet provider = null;

        @Override
        public SelectorSet provider() { return provider; }

        public Signal(SelectableChannel channel, int op) {
            this.channel = channel;
            this.op = op;
        }

        @Override
        public void register(ISignalSet set) throws Exception {
            if(provider != null)
                throw new IllegalArgumentException();

            if(!(set instanceof SelectorSet))
                throw new IllegalArgumentException();

            provider = (SelectorSet)set;
        }

        private void register(Selector selector) throws Exception {
            key = channel.register(selector, 0);
            key.attach(this);
        }

        @Override
        public void close() throws java.io.IOException {
            cancel();
            super.close();
        }

        @Override
        public void cancel() {
            if(key != null)
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

        boolean persistent = true;

        @Override
        public boolean persistent() { return persistent; }
        @Override
        public void persistent(boolean value) { persistent = value; }

        @Override
        public void handle(Object source) throws Exception {
            super.handle(source);
            
            if(!persistent())
                stop();
        }
    }

    @Override
    public void conj(ISignal signal) throws Exception {
        if(!(signal instanceof Signal))
            throw new IllegalArgumentException();

        ((Signal)signal).register(selector);
        signal.register(this);
    }

    @Override
    public void disj(ISignal signal) {
        signal.cancel();
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

    public void interrupt() throws Exception {
        selector.wakeup();
        selectNow();
    }
}
