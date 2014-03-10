package madnet.event;

public interface ISignal extends IEvent {
    ISignalSet provider();

    void register(ISignalSet set) throws Exception;
    void cancel();

    boolean persistent();
    void persistent(boolean value);

    void stop();
}
