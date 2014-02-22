package madnet.event;

public interface IEventSet {
    //void cancel();

    java.util.Collection<? extends IEvent> events();
    Iterable<? extends IEvent> selections();
    
    //void selectNow();

    //void selectIn(int milliseconds);
    //void selectIn(long milliseconds);
    //void selectIn(double timeout);

    void select() throws InterruptedException;

    //void interrupt();
}
