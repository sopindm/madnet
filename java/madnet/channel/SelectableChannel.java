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
    extends madnet.channel.Channel {
    protected T channel = null;
    protected Events events = null;

    public SelectableChannel(T ch) throws java.io.IOException {
        channel = ch;
        channel.configureBlocking(false);

        Event onClose = new Event();
        onClose.oneShot(true);
        onClose.attach(this);

        events = new Events(null, null, onClose);
    }

    @Override
    public boolean isOpen() {
        return channel.isOpen();
    }

    @Override
    public void close() throws java.io.IOException {
        Exception exception = null;

        try {
            events().onClose().emit();
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

        try {
            events().close();
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

    @Override
    public Events events() {
        return events;
    }

    @Override
    public boolean tryPush(Object obj) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object tryPop() throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public Result read(IChannel ch) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public Result write(IChannel ch) throws Exception {
        throw new UnsupportedOperationException();
    }
}
