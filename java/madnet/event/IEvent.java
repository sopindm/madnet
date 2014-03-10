package madnet.event;

public interface IEvent extends IEventHandler, ISet<IEventHandler> {
    void emit() throws Exception;

    void handle() throws Exception;
    void handle(Object source) throws Exception;

    Iterable<IEventHandler> handlers();

    Object attachment();
    void attach(Object attachment);
}
