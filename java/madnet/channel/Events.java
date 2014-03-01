package madnet.channel;

import madnet.event.IEvent;

public class Events {
    final IEvent onRead;
    final IEvent onWrite;

    final IEvent onReadClosed;
    final IEvent onWriteClosed;

    final IEvent onClosed;

    public Events(IEvent onRead,
                  IEvent onWrite,
                  IEvent onReadClosed,
                  IEvent onWriteClosed,
                  IEvent onClosed) {
        this.onRead = onRead;
        this.onWrite = onWrite;
        this.onReadClosed = onReadClosed;
        this.onWriteClosed = onWriteClosed;
        this.onClosed = onClosed;
    }

    public IEvent onRead() { return onRead; }
    public IEvent onWrite() { return onWrite; }

    public IEvent onReadClosed() { return onReadClosed; }
    public IEvent onWriteClosed() { return onWriteClosed; }
    public IEvent onClosed() { return onClosed; }

    public static final Events EMPTY = new Events(null, null, null, null, null);
}
