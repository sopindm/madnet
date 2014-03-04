package madnet.event;

public interface IEvent {
    public void pushHandler(IEventHandler handler);
    public void popHandler(IEventHandler handler);
}
