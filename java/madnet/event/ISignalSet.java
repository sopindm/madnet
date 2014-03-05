package madnet.event;

public interface ISignalSet extends java.io.Closeable {
    boolean isOpen();

    Iterable<? extends ISignal> signals();
    Iterable<? extends ISignal> selections();

    boolean isEmpty();

    ISignalSet push(ISignal signal) throws Exception;
    void pop(ISignal signal);
    
    ISignalSet select() throws Exception;
    ISignalSet selectIn(long milliseconds) throws Exception;
    ISignalSet selectNow() throws Exception;
}
