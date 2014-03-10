package madnet.event;

import java.nio.channels.ClosedSelectorException;
import java.util.Iterator;
import java.util.AbstractSet;
import java.util.HashSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class TriggerSet extends SignalSet<TriggerSet.Signal> {
    LinkedBlockingQueue<Signal> triggered = new LinkedBlockingQueue<Signal>();

    public static class Signal extends madnet.event.Signal {
        @Override
        public TriggerSet provider() { 
            return (TriggerSet)super.provider();
        }

        @Override
        public void register(ISignalSet provider) {
            super.register(provider);

            if(!(this.provider instanceof TriggerSet))
                throw new IllegalArgumentException();
        }

        @Override
        public void emit() {
            if(provider() != null)
                provider().triggered.add(this);
            else
                throw new UnsupportedOperationException();
        }

        @Override
        public void stop() {
        }

        private void cancelProvider() {
            provider = null;
        }
    }

    @Override
    public void close() {
        for(Signal s : signals())
            s.cancelProvider();

        triggered.clear();
        super.close();
    }

    @Override
    public void conj(ISignal signal) {
        if(!isOpen())
            throw new ClosedSelectorException();

        if(!(signal instanceof Signal))
            throw new IllegalArgumentException();

        signals().add((Signal)signal);
        ((Signal)signal).register(this);
    }

    @Override
    public void disj(ISignal signal) {
        if(!isOpen())
            throw new ClosedSelectorException();

        if(!(signal instanceof Signal))
            throw new IllegalArgumentException();

        canceling.add((Signal)signal);
    }

    private void cancelSignals() {
        while(!canceling.isEmpty()) {
            Signal s = canceling.poll();

            if(s != null) {
                signals().remove(s);
                selections().remove(s);

                s.cancel();
            }
        }
    }

    private void pushSelection(Signal e) {
        if(e == null)
            return;

        selections().add(e);
    }

    @Override
    public TriggerSet select() throws InterruptedException {
        if(!isOpen())
            throw new ClosedSelectorException();

        cancelSignals();

        if(selections().size() == 0 && signals.size() > 0)
            pushSelection(triggered.take());

        selectNow();
        return this;
    }

    @Override
    public TriggerSet selectIn(long milliseconds) throws InterruptedException {
        if(!isOpen())
            throw new ClosedSelectorException();

        cancelSignals();

        if(selections().size() == 0 && signals.size() > 0)
            pushSelection(triggered.poll(milliseconds, TimeUnit.MILLISECONDS));

        selectNow();
        return this;
    }

    @Override
    public TriggerSet selectNow() {
        if(!isOpen())
            throw new ClosedSelectorException();

        cancelSignals();

        while(triggered.size() > 0) {
            pushSelection(triggered.poll());
        }

        return this;
    }
}
