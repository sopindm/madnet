package madnet.event;

public interface IEvent extends IEventHandler, ISet<IEventHandler> {
    public void emit();

    Iterable<IEventHandler> handlers();

    Object attachment();
    void attach(Object attachment);
}
