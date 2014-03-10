package madnet.event;

import java.nio.channels.ClosedSelectorException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

public class TimerSet extends SignalSet<TimerSet.Signal> {
    SortedMap<Long, LinkedList<Signal>> timeouts = new java.util.TreeMap<Long, LinkedList<Signal>>();

    public Long timeout() {
        if(timeouts.size() == 0)
            return null;

        return Math.max(0, timeouts.firstKey() - System.currentTimeMillis());
    }

    public static class Signal extends madnet.event.Signal {
        long timeout;
        long finishStamp = 0;

        public Signal(long millisecondsTimeout) {
            timeout = millisecondsTimeout;
        }

        public long timeout() {
            return timeout;
        }

        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }

        @Override
        public TimerSet provider() { 
            return (TimerSet)super.provider();
        }

        @Override
        public void register(ISignalSet provider) {
            super.register(provider);

            if(!(this.provider instanceof TimerSet))
                throw new IllegalArgumentException();
        }

        @Override
        public void emit() {
            finishStamp = System.currentTimeMillis() + timeout;

            LinkedList<Signal> entry = provider().timeouts.get(finishStamp);

            if(entry == null) {
                entry = new LinkedList<Signal>();
                provider().timeouts.put(finishStamp, entry);
            }

            entry.add(this);
        }

        @Override
        public void cancel() {
            stop();
            super.cancel();
        }

        @Override
        public void stop() {
            if(provider() == null)
                return;

            LinkedList<Signal> signal = provider().timeouts.get(finishStamp);
            if(signal == null)
                return;

            if(signal.size() == 1)
                provider().timeouts.remove(finishStamp);
            else
                signal.remove(this);

            finishStamp = 0;
        }

        private void cancelProvider() {
            provider = null;
        }
    }

    @Override
    public void close() {
        for(Signal e : signals())
            e.cancelProvider();

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

    private void pushSelection(Signal s) {
        if(s == null)
            return;

        selections().add(s);
    }

    @Override
    public TimerSet select() throws InterruptedException {
        if(!isOpen())
            throw new ClosedSelectorException();

        cancelSignals();

        long timestamp = System.currentTimeMillis();

        if(selections().size() == 0 && signals().size() > 0
           && timeouts.size() > 0
           && timeouts.firstKey() > timestamp)
            Thread.sleep(timeouts.firstKey() - timestamp);
        
        selectNow();
        return this;
    }

    @Override
    public TimerSet selectIn(long milliseconds) throws InterruptedException {
        if(!isOpen())
            throw new ClosedSelectorException();

        cancelSignals();

        long timestamp = System.currentTimeMillis();

        if(selections().size() == 0 && signals().size() > 0 && timeouts.size() > 0
           && timeouts.firstKey() > timestamp) {
            Thread.sleep(Math.min(timeouts.firstKey() - timestamp, milliseconds));
        }

        selectNow();
        return this;
    }

    @Override
    public TimerSet selectNow() {
        if(!isOpen())
            throw new ClosedSelectorException();

        cancelSignals();

        long timestamp = System.currentTimeMillis();

        Iterator<Map.Entry<Long, LinkedList<Signal>>> iterator = timeouts.entrySet().iterator();

        while(iterator.hasNext()) {
            Map.Entry<Long, LinkedList<Signal>> entry = iterator.next();

            if(entry.getKey() <= timestamp) {
                for(Signal e : entry.getValue())
                    selections().add(e);

                iterator.remove();
            }
            else
                break;
        }

        return this;
    }
}
