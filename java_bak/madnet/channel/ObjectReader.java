package madnet.channel;

import java.util.Deque;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.Pipe;
import madnet.event.ISignalSet;

public class ObjectReader extends AChannel {
    ObjectWire wire;
    boolean isOpen = true;

    @Override
    public boolean isOpen() {
        return isOpen;
    }

    @Override
    public void close() throws java.io.IOException {
        try {
            super.close();
        }
        finally {
            isOpen = false;
            wire.closeRead();
        }
    }

    public ObjectReader(ObjectWire wire) throws Exception {
        this.wire = wire;
    }

    @Override
    public void register(ISignalSet set) throws Exception {
        if(!isOpen)
            throw new ClosedChannelException();
    }

    @Override
    public Object tryPop() throws Exception {
        if(!isOpen)
            throw new ClosedChannelException();

        Object obj = wire.pop();

        if(obj == null && !wire.writable())
            close();

        return obj;
    }

    @Override
    public Result read(IChannel ch) throws Exception {
        if(!isOpen)
            throw new ClosedChannelException();

        int read = 0;
        Object o = wire.fetch();

        while(o != null && ch.tryPush(o)) {
            wire.commitFetch();
            read++;

            o = wire.fetch();
        }

        if(o != null)
            wire.cancelFetch(o);

        if(o == null && !wire.writable())
            close();

        return new Result(read);
    }
}
