package madnet.event;

public abstract class Signal extends AEvent implements ISignal {
    protected SignalSet provider = null;

    @Override
    public SignalSet provider() { return provider; };

    @Override
    public void register(ISignalSet provider) throws Exception {
        if(this.provider != null)
            throw new IllegalArgumentException();
        
        this.provider = (SignalSet)provider.push(this);
    }

    @Override
    public void handle() {
        for(IEventHandler h : handlers())
            h.onCallback(this);
    }

    @Override
    public void close() throws java.io.IOException {
        cancel();
    }

    @Override
    public void cancel() throws java.io.IOException {
        if(provider != null) {
            provider.pop(this);
            provider = null;
        }
    }
}
