package madnet.channel;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import madnet.event.Event;
import madnet.event.ISignal;
import madnet.event.SelectorSignal;
import madnet.channel.IChannel;
import madnet.channel.Result;
import madnet.channel.Events;
import madnet.range.nio.ByteRange;

public abstract class SelectableChannel<T extends java.nio.channels.SelectableChannel>
    extends madnet.channel.AChannel {
    protected T channel = null;

    public SelectableChannel(T ch) throws java.io.IOException {
        super();

        channel = ch;
        channel.configureBlocking(false);
    }

    public T channel() { return channel; }

    @Override
    public boolean isOpen() {
        return channel.isOpen();
    }

    @Override
    public void close() throws java.io.IOException {
        Exception exception = null;

        try {
            super.close();
        }
        catch(Exception e) {
            exception = e;
        }

        try {
            if(channel != null)
                channel.close();
        }
        catch(Exception e) {
            if(exception == null)
                exception = e;
        }

        if(exception != null)
            throw new RuntimeException(exception);
    }

    @Override
    public SelectableChannel clone() throws CloneNotSupportedException {
        return (SelectableChannel)super.clone();
    }
}
