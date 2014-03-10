package madnet.event;

public abstract class Signal extends Event implements ISignal {
    protected SignalSet<? extends Signal> provider = null;
    public SignalSet<? extends Signal> provider() { return provider; };

    @Override
    public void register(ISignalSet provider) throws Exception {
        if(this.provider != null)
            throw new IllegalArgumentException();
        
        this.provider = (SignalSet<? extends Signal>)provider;
    }

    @Override
    public void handle() {
        for(IEventHandler h : handlers())
            h.call(this, attachment);
    }

    @Override
    public void close() throws java.io.IOException {
        cancel();
        attachment = null;
    }

    @Override
    public void cancel() throws java.io.IOException {
        if(provider != null) {
            provider.disj((ISignal)this);
            provider = null;
        }
    }
}
