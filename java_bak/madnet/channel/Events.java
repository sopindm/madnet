package madnet.channel;

import madnet.event.ISignal;
import madnet.event.IEvent;

public class Events implements IEvents {
    ISignal onRead;
    ISignal onWrite;
    IEvent onClose;

    public Events(ISignal onRead, ISignal onWrite, IEvent onClose) {
        this.onRead = onRead;
        this.onWrite = onWrite;
        this.onClose = onClose;
    }

    public void close() throws java.io.IOException {
        Exception exception = null;

        if(onRead != null) {
            try {
                onRead.close();
            }
            catch(Exception e) {
                exception = e;
            }
        }

        if(onWrite != null) {
            try {
                onWrite.close();
            }
            catch(Exception e) {
                exception = e;
            }
        }

        if(onClose != null) {
            try {
                onClose.close();
            }
            catch(Exception e) {
                exception = e;
            }
        }

        if(exception != null)
            throw new RuntimeException(exception);
    }

    @Override
    public ISignal onRead() { return onRead; }
    public void onRead(ISignal signal) { onRead = signal; }

    @Override
    public ISignal onWrite() { return onWrite; }
    public void onWrite(ISignal signal) { onWrite = signal; }

    @Override
    public IEvent onClose() { return onClose; }
    public void onClose(ISignal signal) { onClose = signal; }

    public static final Events EMPTY = new Events(null, null, null);
}
