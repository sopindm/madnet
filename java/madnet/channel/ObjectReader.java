package madnet.channel;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.nio.channels.Pipe;
import madnet.event.ISignalSet;

public class ObjectReader extends AChannel {
    ConcurrentLinkedQueue<Object> wire;
    boolean isOpen = true;

    @Override
    public boolean isOpen() {
        return isOpen;
    }

    public ObjectReader(ConcurrentLinkedQueue<Object> wire) throws Exception {
        this.wire = wire;
    }

    @Override
    public void register(ISignalSet set) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object tryPop() throws Exception {
        return wire.poll();
    }
}
