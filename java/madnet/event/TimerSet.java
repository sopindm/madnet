package madnet.event;

import java.nio.channels.ClosedSelectorException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

public class TimerSet extends EventSet<TimerSet.Event> {
    SortedMap<Long, LinkedList<Event>> timeouts = new java.util.TreeMap<Long, LinkedList<Event>>();

    public Long timeout() {
        if(timeouts.size() == 0)
            return null;

        return timeouts.firstKey() - System.currentTimeMillis();
    }

    public static class Event extends madnet.event.Event {
        long timeout;
        long finishStamp = 0;

        public Event(long millisecondsTimeout) {
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
        public void register(IEventSet provider) throws Exception {
            super.register(provider);

            if(!(this.provider instanceof TimerSet))
                throw new IllegalArgumentException();
        }

        @Override
        public void start() {
            finishStamp = System.currentTimeMillis() + timeout;

            LinkedList<Event> entry = provider().timeouts.get(finishStamp);

            if(entry == null) {
                entry = new LinkedList<Event>();
                provider().timeouts.put(finishStamp, entry);
            }

            entry.add(this);
        }

        @Override
        public void cancel() throws java.io.IOException {
            stop();
            super.cancel();
        }

        @Override
        public void stop() {
            LinkedList<Event> event = provider().timeouts.get(finishStamp);
            if(event == null)
                return;

            if(event.size() == 1)
                provider().timeouts.remove(finishStamp);
            else
                event.remove(this);

            finishStamp = 0;
        }

        private void cancelProvider() {
            provider = null;
        }
    }

    @Override
    public void close() {
        for(Event e : events())
            e.cancelProvider();

        super.close();
    }

    @Override
    public TimerSet push(IEvent event) {
        if(!isOpen())
            throw new ClosedSelectorException();

        if(!(event instanceof Event))
            throw new IllegalArgumentException();

        events().add((Event)event);
        return this;
    }

    @Override
    public void pop(IEvent event) {
        if(!isOpen())
            throw new ClosedSelectorException();

        if(!(event instanceof Event))
            throw new IllegalArgumentException();

        canceling.add((Event)event);
    }

    private void cancelEvents() {
        while(!canceling.isEmpty()) {
            Event e = canceling.poll();

            if(e != null) {
                events().remove(e);
                selections().remove(e);
            }
        }
    }

    private void pushSelection(Event e) {
        if(e == null)
            return;

        selections().add(e);
    }

    Thread selectionThread = null;

    @Override
    public TimerSet select() {
        if(!isOpen())
            throw new ClosedSelectorException();

        cancelEvents();

        long timestamp = System.currentTimeMillis();

        if(selections().size() == 0 && events.size() > 0 && timeouts.size() > 0
           && timeouts.firstKey() > timestamp) {
            try {
                selectionThread = Thread.currentThread();
                Thread.sleep(timeouts.firstKey() - timestamp);
            }
            catch(InterruptedException e) {
            }
            finally {
                selectionThread = null;
            }
        }
        
        selectNow();
        return this;
    }

    @Override
    public TimerSet selectIn(long milliseconds) {
        if(!isOpen())
            throw new ClosedSelectorException();

        cancelEvents();

        long timestamp = System.currentTimeMillis();

        if(selections().size() == 0 && events.size() > 0 && timeouts.size() > 0
           && timeouts.firstKey() > timestamp) {
            try {
                selectionThread = Thread.currentThread();
                Thread.sleep(Math.min(timeouts.firstKey() - timestamp, milliseconds));
            }
            catch(InterruptedException e) {
            }
            finally {
                selectionThread = null;
            }
        }

        selectNow();
        return this;
    }

    @Override
    public TimerSet selectNow() {
        if(!isOpen())
            throw new ClosedSelectorException();

        cancelEvents();

        long timestamp = System.currentTimeMillis();

        Iterator<Map.Entry<Long, LinkedList<Event>>> iterator = timeouts.entrySet().iterator();

        while(iterator.hasNext()) {
            Map.Entry<Long, LinkedList<Event>> entry = iterator.next();

            if(entry.getKey() <= timestamp) {
                for(Event e : entry.getValue())
                    selections().add(e);

                iterator.remove();
            }
            else
                break;
        }

        return this;
    }

    @Override
    public void interrupt() {
        if(selectionThread != null) {
            Thread interruptedThread = selectionThread;

            if(interruptedThread != null)
                interruptedThread.interrupt();
        }
    }
}
