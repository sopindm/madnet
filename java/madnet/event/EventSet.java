package madnet.event;

public interface EventSet {
    EventSet push(IEvent event);
    EventSet pop(IEvent event);

    Iterable selectNow();

    Iterable selectIn(int milliseconds);
    Iterable selectIn(long milliseconds);
    Iterable selectIn(double timeout);

    Iterable select();

    void interupt();
}
