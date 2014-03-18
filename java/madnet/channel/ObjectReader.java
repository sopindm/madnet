package madnet.channel;

import java.util.Deque;
import java.nio.channels.Pipe;
import madnet.event.ISignalSet;

public class ObjectReader extends AChannel {
    ObjectWire wire;
    boolean isOpen = true;

    @Override
    public boolean isOpen() {
        return isOpen;
    }

    public ObjectReader(ObjectWire wire) throws Exception {
        this.wire = wire;
    }

    @Override
    public void register(ISignalSet set) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object tryPop() throws Exception {
        return wire.pop();
    }

    @Override
    public Result read(IChannel ch) throws Exception {
        int read = 0;
        Object o = wire.fetch();

        while(o != null && ch.tryPush(o)) {
            wire.commitFetch();
            read++;

            o = wire.pop();
        }

        if(o != null)
            wire.cancelFetch(o);

        return new Result(read);
    }
}
