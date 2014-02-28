package madnet.event;

import java.nio.channels.ClosedSelectorException;
import java.util.Iterator;
import java.util.AbstractSet;
import java.util.HashSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class TriggerSet extends EventSet<TriggerSet.Event> {
    LinkedBlockingQueue<Event> triggered = new LinkedBlockingQueue<Event>();

    public static class Event extends madnet.event.Event {
        @Override
        public TriggerSet provider() { 
            return (TriggerSet)super.provider();
        }

        @Override
        public void register(IEventSet provider) throws Exception {
            super.register(provider);

            if(!(this.provider instanceof TriggerSet))
                throw new IllegalArgumentException();
        }

        @Override
        public void start() {
            provider().triggered.add(this);
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
        for(Event e : events())
            e.cancelProvider();

        triggered.clear();

        super.close();
    }

    @Override
    public TriggerSet push(IEvent event) {
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

        if(e.provider() != this)
            return;

        selections().add(e);
    }

    Thread selectionThread = null;

    @Override
    public TriggerSet select() {
        if(!isOpen())
            throw new ClosedSelectorException();

        cancelEvents();

        if(selections().size() == 0 && events.size() > 0) {
            try {
                selectionThread = Thread.currentThread();
                pushSelection(triggered.take());
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
    public TriggerSet selectIn(long milliseconds) {
        if(!isOpen())
            throw new ClosedSelectorException();

        cancelEvents();

        if(selections().size() == 0 && events.size() > 0) {
            try {
                selectionThread = Thread.currentThread();
                pushSelection(triggered.poll(milliseconds, TimeUnit.MILLISECONDS));
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
    public TriggerSet selectNow() {
        if(!isOpen())
            throw new ClosedSelectorException();

        cancelEvents();

        while(triggered.size() > 0) {
            pushSelection(triggered.poll());
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
