package madnet.event;

public interface ISignal extends IEvent, java.io.Closeable {
    void register(ISignalSet set) throws Exception;
    void cancel() throws Exception;

    void start();
    void stop();

    void handle();

    ISignalSet provider();
}
