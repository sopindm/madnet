package madnet.event;

public interface IEventHandler extends java.io.Closeable {
    public void call(Object emitter, Object source);

    public void subscribe(IEvent event);
    public void unsubscribe(IEvent event);
}
