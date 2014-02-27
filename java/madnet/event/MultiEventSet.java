package madnet.event;

public class MultiEventSet implements IEventSet {
    TriggerSet triggers;
    TimerSet timers;
    SelectorSet selectors;

    public MultiEventSet() throws Exception {
        triggers =  new TriggerSet();
        timers = new TimerSet();
        selectors = new SelectorSet();
    }

    public TriggerSet triggers() { return triggers; }
    public TimerSet timers() { return timers; }
    public SelectorSet selectors() { return selectors; }

    @Override
    public void close() throws java.io.IOException {
        throw new UnsupportedOperationException();
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

    @Override
    public MultiEventSet select() throws Exception {
        triggers.select();
        return this;
    }

    @Override
    public MultiEventSet selectIn(long milliseconds) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public MultiEventSet selectNow() throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void interrupt() {
        throw new UnsupportedOperationException();
    }
} 
