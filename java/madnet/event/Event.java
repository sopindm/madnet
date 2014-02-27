package madnet.event;

public abstract class Event implements IEvent {
    protected EventSet provider = null;

    @Override
    public EventSet provider() { return provider; };

    @Override
    public void register(IEventSet provider) throws Exception {
        if(this.provider != null)
            throw new IllegalArgumentException();
        
        this.provider = (EventSet)provider.push(this);
    }

    @Override
    public void close() throws java.io.IOException {
        cancel();
        attachment = null;
    }

    @Override
    public void cancel() throws java.io.IOException {
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
