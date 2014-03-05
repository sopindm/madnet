package madnet.event;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MultiSignalSet implements ISignalSet {
    final TriggerSet triggers;
    final TimerSet timers;
    final SelectorSet selectors;

    final ExecutorService executor;

    public MultiSignalSet() throws Exception {
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
    public Iterable<ISignal> signals() {
        return new madnet.util.MultiIterable<ISignal>(triggers.signals(),
                                                      timers.signals(),
                                                      selectors.signals());
    }

    @Override
    public Iterable<ISignal> selections() {
        return new madnet.util.MultiIterable<ISignal>(triggers.selections(),
                                                     timers.selections(),
                                                     selectors.selections());
    }

    @Override
    public ISignalSet push(ISignal signal) throws Exception {
        if(signal instanceof TriggerSet.Signal)
            return triggers.push(signal);

        if(signal instanceof TimerSet.Signal)
            return timers.push(signal);

        if(signal instanceof SelectorSet.Signal)
            return selectors.push(signal);

        throw new IllegalArgumentException();
    }

    @Override
    public void pop(ISignal signal) {
        throw new UnsupportedOperationException();
    }

    MultiSignalSet selectImpl() throws Exception {
        if(selectors.isEmpty()) {
            triggers.select();
        }
        else if (triggers.isEmpty()) {
            selectors.select();
        }
        else {
            Future f = executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            triggers.select();

                            try {
                                selectors.interrupt();
                            }
                            catch(Exception e) {
                            }}
                        catch(InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }}});
            
            selectors.select();
            f.cancel(true);
        }

        return this;
    }

    @Override
    public MultiSignalSet select() throws Exception {
        Long timeout = timers.timeout();

        if(timeout != null) {
            if(timeout <= 0)
                return selectNow();

            return selectInImpl(timeout);
        }

        return selectImpl();
    }

    MultiSignalSet selectInImpl(final long timeout) throws Exception {
        if(triggers.isEmpty()) {
            selectors.selectIn(timeout);
        }
        else if(selectors.isEmpty()) {
            triggers.selectIn(timeout);
        }
        else {
            Future f = executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            triggers.selectIn(timeout);
                        
                            try {
                                selectors.interrupt();
                            }
                            catch(Exception e) {
                            }}
                        catch(InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }}});

            selectors.selectIn(timeout);
            f.cancel(true);
        }

        timers.selectNow();
        
        return this;
    }

    @Override
    public MultiSignalSet selectIn(long milliseconds) throws Exception {
        Long timeout = timers.timeout();

        if(timeout != null)
            milliseconds = Math.min(timeout, milliseconds);

        return selectInImpl(milliseconds);
    }

    @Override
    public MultiSignalSet selectNow() throws Exception {
        timers.selectNow();
        triggers.selectNow();
        selectors.selectNow();

        return this;
    }
} 
