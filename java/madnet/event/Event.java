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

    Object attachment = null;

    @Override
    public Object attachment() { return attachment; }

    @Override
    public void attach(Object value) { attachment = value; }
}
