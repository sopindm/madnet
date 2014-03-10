package madnet.event;

public interface ISignalSet extends java.io.Closeable, ISet<ISignal> {
    boolean isOpen();

    Iterable<? extends ISignal> signals();
    Iterable<? extends ISignal> selections();

    boolean isEmpty();

    ISignalSet select() throws Exception;
    ISignalSet selectIn(long milliseconds) throws Exception;
    ISignalSet selectNow() throws Exception;
}
