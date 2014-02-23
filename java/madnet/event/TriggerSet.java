package madnet.event;

import java.util.Iterator;
import java.util.AbstractSet;
import java.util.HashSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class TriggerSet extends EventSet<Event> {
    LinkedBlockingQueue<Event> triggered;

    public TriggerSet() {
        triggered = new LinkedBlockingQueue<Event>();
    }

    public static class Event extends madnet.event.Event {
        @Override
        public TriggerSet provider() { 
            return (TriggerSet)super.provider();
        }

        public void touch() {
            provider().triggered.add(this);
        }
    }

    @Override
    public void push(IEvent event) {
        if(!(event instanceof Event))
            throw new IllegalArgumentException();

        events().add((Event)event);
    }

    Thread selectionThread = null;

    @Override
    public TriggerSet select() {
        if(selections().size() == 0) {
            try {
                selectionThread = Thread.currentThread();
                selections().add(triggered.take());
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
        if(selections().size() == 0) {
            Event event = null;
            try {
                selectionThread = Thread.currentThread();
                event = triggered.poll(milliseconds, TimeUnit.MILLISECONDS);
            }
            catch(InterruptedException e) {
            }
            finally {
                selectionThread = null;
            }

            if(event == null)
                return this;

            selections().add(event);
        }

        selectNow();
        return this;
    }

    @Override
    public TriggerSet selectNow() {
        while(triggered.size() > 0) {
            Event event = triggered.poll();

            if(event != null)
                selections().add(event);
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
