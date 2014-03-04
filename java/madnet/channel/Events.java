package madnet.channel;

import madnet.event.ISignal;

public class Events {
    final ISignal onRead;
    final ISignal onWrite;

    final ISignal onReadClosed;
    final ISignal onWriteClosed;

    final ISignal onClosed;

    public Events(ISignal onRead,
                  ISignal onWrite,
                  ISignal onReadClosed,
                  ISignal onWriteClosed,
                  ISignal onClosed) {
        this.onRead = onRead;
        this.onWrite = onWrite;
        this.onReadClosed = onReadClosed;
        this.onWriteClosed = onWriteClosed;
        this.onClosed = onClosed;
    }

    public ISignal onRead() { return onRead; }
    public ISignal onWrite() { return onWrite; }

    public ISignal onReadClosed() { return onReadClosed; }
    public ISignal onWriteClosed() { return onWriteClosed; }
    public ISignal onClosed() { return onClosed; }

    public static final Events EMPTY = new Events(null, null, null, null, null);
}
