package madnet.channel;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.nio.channels.Pipe;
import madnet.event.ISignalSet;

public class ObjectWriter extends AChannel {
    ConcurrentLinkedQueue<Object> wire;
    boolean isOpen = true;

    @Override
    public boolean isOpen() {
        return isOpen;
    }

    public ObjectWriter(ConcurrentLinkedQueue<Object> wire) throws Exception {
        super();
        this.wire = wire;
    }

    @Override
    public void register(ISignalSet set) {
        throw new UnsupportedOperationException();
    }

    public boolean tryPush(Object o) throws Exception {
        wire.add(o);
        return true;
    }
}
