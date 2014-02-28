package madnet.event;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MultiEventSet implements IEventSet {
    final TriggerSet triggers;
    final TimerSet timers;
    final SelectorSet selectors;

    final ExecutorService executor;

    public MultiEventSet() throws Exception {
        triggers =  new TriggerSet();
        timers = new TimerSet();
        selectors = new SelectorSet();

        executor = Executors.newFixedThreadPool(1);
    }

    public TriggerSet triggers() { return triggers; }
    public TimerSet timers() { return timers; }
    public SelectorSet selectors() { return selectors; }

    @Override
    public boolean isEmpty() {
        return triggers.isEmpty() && timers.isEmpty() && selectors.isEmpty();
    }

    @Override
    public void close() throws java.io.IOException {
        triggers.close();
        timers.close();
        selectors.close();
    }

    @Override
    public boolean isOpen() {
        return triggers.isOpen() && timers.isOpen() && selectors.isOpen();
    }

    @Override
    public Iterable<IEvent> events() {
        return new madnet.util.MultiIterable<IEvent>(triggers.events(),
                                                     timers.events(),
                                                     selectors.events());
    }

    @Override
    public Iterable<IEvent> selections() {
        return new madnet.util.MultiIterable<IEvent>(triggers.selections(),
                                                     timers.selections(),
                                                     selectors.selections());
    }

    @Override
    public IEventSet push(IEvent event) throws Exception {
        if(event instanceof TriggerSet.Event)
            return triggers.push(event);

        if(event instanceof TimerSet.Event)
            return timers.push(event);

        if(event instanceof SelectorSet.Event)
            return selectors.push(event);

        throw new IllegalArgumentException();
    }

    @Override
    public void pop(IEvent event) {
        throw new UnsupportedOperationException();
    }

    MultiEventSet selectImpl() throws Exception {
        if(selectors.isEmpty()) {
            triggers.select();
        }
        else if (triggers.isEmpty()) {
            selectors.select();
        }
        else {
            executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        triggers.select();
                        try {
                            selectors.interrupt();
                        }
                        catch(Exception e) {
                        }}});
            
            selectors.select();
            triggers.interrupt();
        }

        return this;
    }

    @Override
    public MultiEventSet select() throws Exception {
        Long timeout = timers.timeout();

        if(timeout != null) {
            if(timeout <= 0)
                return selectNow();

            return selectInImpl(timeout);
        }

        return selectImpl();
    }

    MultiEventSet selectInImpl(final long timeout) throws Exception {
        if(triggers.isEmpty()) {
            selectors.selectIn(timeout);
        }
        else if(selectors.isEmpty()) {
            triggers.selectIn(timeout);
        }
        else {
            executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        triggers.selectIn(timeout);
                        
                        try {
                            selectors.interrupt();
                        }
                        catch(Exception e) {
                        }}});

            selectors.selectIn(timeout);
            triggers.interrupt();
        }

        timers.selectNow();
        
        return this;
    }

    @Override
    public MultiEventSet selectIn(long milliseconds) throws Exception {
        Long timeout = timers.timeout();

        if(timeout != null)
            milliseconds = Math.min(timeout, milliseconds);

        return selectInImpl(milliseconds);
    }

    @Override
    public MultiEventSet selectNow() throws Exception {
        timers.selectNow();
        triggers.selectNow();
        selectors.selectNow();

        return this;
    }

    @Override
    public void interrupt() throws Exception {
        triggers.interrupt();
        selectors.interrupt();
    }
} 
