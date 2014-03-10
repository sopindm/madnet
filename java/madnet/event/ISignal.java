package madnet.event;

public interface ISignal extends IEvent {
    void register(ISignalSet set);
    void cancel();

    void stop();
}
