package madnet.event;

public interface IEvent extends IEventHandler, ISet<IEventHandler> {
    void emit(Object obj) throws Exception;

    Iterable<IEventHandler> handlers();
}
