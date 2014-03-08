package madnet.channel;

import madnet.event.ISignal;
import madnet.event.IEvent;

public class Events implements IEvents {
    final ISignal onRead;
    final ISignal onWrite;
    final IEvent onClose;

    public Events(ISignal onRead, ISignal onWrite, IEvent onClose) {
        this.onRead = onRead;
        this.onWrite = onWrite;
        this.onClose = onClose;
    }

    @Override
    public ISignal onRead() { return onRead; }
    @Override
    public ISignal onWrite() { return onWrite; }
    @Override
    public IEvent onClose() { return onClose; }

    public static final Events EMPTY = new Events(null, null, null);
}
