package madnet.event;

public abstract class Event implements IEvent {
    protected EventSet provider = null;

    @Override
    public EventSet provider() { return provider; };

    @Override
    public void register(IEventSet provider) {
        if(this.provider != null)
            throw new IllegalArgumentException();
        
        provider.push(this);
        this.provider = (EventSet)provider;
    }

    @Override
    public void close() {
        cancel();
        attachment = null;
    }

    @Override
    public void cancel() {
        if(provider != null) {
            provider.pop(this);
            provider = null;
        }
    }

    Object attachment = null;

    @Override
    public Object attachment() { return attachment; }

    @Override
    public void attach(Object value) { attachment = value; }
}
