package madnet.channel;

import java.util.Queue;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.Pipe;
import madnet.event.ISignalSet;

public class ObjectWriter extends AChannel {
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
            wire.closeWrite();
        }
    }

    public ObjectWriter(ObjectWire wire) throws Exception {
        super();
        this.wire = wire;
    }

    @Override
    public void register(ISignalSet set) throws Exception {
        if(!isOpen)
            throw new ClosedChannelException();
    }

    public boolean tryPush(Object o) throws Exception {
        if(!isOpen)
            throw new ClosedChannelException();

        if(!wire.readable()) {
            close();
            return false;
        }

        return wire.push(o);
    }

    public Result write(IChannel ch) throws Exception {
        if(!isOpen)
            throw new ClosedChannelException();

        if(!wire.readable()) {
            close();
            return Result.ZERO;
        }

        int writen = 0;

        if(!wire.offer())
            return Result.ZERO;

        Object o = ch.tryPop();

        while(o != null) {
            wire.commitOffer(o);
            writen++;

            if(!wire.offer())
                break;

            o = ch.tryPop();
        }
            
        return new Result(writen);
    }
}
