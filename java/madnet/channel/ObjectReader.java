package madnet.channel;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.nio.channels.Pipe;

public class ObjectReader extends ReadableChannel<Pipe.SourceChannel> {
    ConcurrentLinkedQueue<Object> wire;

    public ObjectReader(Pipe.SourceChannel selectable,
                        ConcurrentLinkedQueue<Object> wire)
        throws Exception {
        super(selectable);

        this.wire = wire;
    }

    @Override
    public Object tryPop() throws Exception {
        if(super.tryPop() == null)
            return false;
            
        return wire.remove();
    }
}
