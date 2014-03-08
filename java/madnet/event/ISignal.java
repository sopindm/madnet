package madnet.event;

public interface ISignal extends IEvent {
    void register(ISignalSet set) throws Exception;
    void cancel() throws Exception;

    void stop();
}
