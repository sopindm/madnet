package madnet.event;

public interface IEvent extends IEventHandler {
    public void emit(Object source);

    Iterable<IEventHandler> handlers();

    public void pushHandler(IEventHandler handler);
    public void popHandler(IEventHandler handler);
}
