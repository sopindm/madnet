package madnet.event;

public interface ISignal extends java.io.Closeable {
    void register(ISignalSet set) throws Exception;
    void cancel() throws Exception;

    void start();
    void stop();

    ISignalSet provider();
}
