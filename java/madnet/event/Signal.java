package madnet.event;

public abstract class Signal extends Event implements ISignal {
    protected SignalSet<? extends Signal> provider = null;
    @Override
    public SignalSet<? extends Signal> provider() { return provider; };

    @Override
    public void register(ISignalSet provider) {
        if(this.provider != null)
            throw new IllegalArgumentException();
        
        this.provider = (SignalSet<? extends Signal>)provider;
    }

    @Override
    public void close() throws java.io.IOException {
        cancel();
        super.close();
    }

    @Override
    public void cancel() {
        if(provider != null) {
            provider.disj((ISignal)this);
            provider = null;
        }
    }

    boolean persistent = false;
    @Override
    public boolean persistent() { return persistent; }
    @Override
    public void persistent(boolean value) { persistent = value; }

    @Override
    public void handle(Object source) throws Exception {
        super.handle(source);

        if(persistent)
            emit();
    }
}
