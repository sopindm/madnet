package madnet.event;

public class FlashSignal extends AEvent implements ISignal {
    @Override
    public void register(ISignalSet set) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void cancel() {
    }

    @Override
    public void close() {
        attachment = null;
    }

    @Override
    public void start() {
        handle();
    }

    @Override
    public void stop() {
    }

    @Override
    public void handle() {
        for(IEventHandler h : handlers())
            h.onCallback(attachment);
    }

    @Override
    public ISignalSet provider() {
        return null;
    }

    Object attachment;
    public void attach(Object attachment) {
        this.attachment = attachment;
    }

    public Object attachment() { return attachment; }
}
