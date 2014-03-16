package madnet.channel;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.nio.channels.Pipe;

public class ObjectWriter extends WritableChannel<Pipe.SinkChannel> {
    ConcurrentLinkedQueue<Object> wire;

    public ObjectWriter(Pipe.SinkChannel selectable,
                        ConcurrentLinkedQueue<Object> wire)
        throws Exception {
        super(selectable);

        this.wire = wire;
    }

    public boolean tryPush(Object o) throws Exception {
        if(!super.tryPush((byte)0))
            return false;
            
        wire.add(o);
        return true;
    }
}
