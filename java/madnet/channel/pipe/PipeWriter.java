package madnet.channel.pipe;

import madnet.event.Event;
import madnet.event.ISignal;
import madnet.event.SelectorSignal;
import madnet.channel.IChannel;
import madnet.channel.Result;
import madnet.channel.Events;
import madnet.range.nio.ByteRange;

public class PipeWriter extends madnet.channel.Channel {
    private java.nio.channels.Pipe.SinkChannel channel = null;
    private Events events = null;

    public PipeWriter(java.nio.channels.Pipe pipe)
        throws java.io.IOException {
        channel = pipe.sink();
        channel.configureBlocking(false);

        ISignal onWrite =
            new SelectorSignal(pipe.sink(),
                               java.nio.channels.SelectionKey.OP_WRITE);
        onWrite.attach(this);

        Event onClose = new Event();
        onClose.oneShot(true);
        onClose.attach(this);

        events = new Events(null, onWrite, onClose);
    }

    @Override
    public boolean isOpen() {
        return channel.isOpen();
    }

    @Override
    public void close() throws java.io.IOException {
        events().onWrite().close();
        channel.close();

        try {
            events().onClose().emit();
        }
        catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public PipeWriter clone() throws CloneNotSupportedException {
        return (PipeWriter)super.clone();
    }

    @Override
    public Events events() {
        return events;
    }

    @Override 
    public void register(madnet.event.ISignalSet set) throws Exception {
        set.conj(events().onWrite());
        events().onWrite().emit();
    }

    @Override
    public boolean tryPush(Object obj) throws Exception {
        if(!(obj instanceof Byte))
            throw new IllegalArgumentException();

        byte[] bytes = new byte[1];
        bytes[0] = (byte)obj;

        java.nio.ByteBuffer src = java.nio.ByteBuffer.wrap(bytes);
        channel.write(src);

        if(src.limit() - src.position() > 0)
            return false;

        return true;
    }

    @Override
    public Object tryPop() throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public Result read(IChannel ch) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Result write(IChannel ch) throws Exception {
        if(!isOpen())
            throw new java.nio.channels.ClosedChannelException();

        if(!(ch instanceof ByteRange))
            return null;

        ByteRange range = (ByteRange)ch;

        java.nio.ByteBuffer buffer = range.buffer();
        int begin = buffer.position();

        try {
            channel.write(buffer);
        } catch(java.io.IOException e) {
            close();
        }

        return new Result(buffer.position() - begin,
                          buffer.position() - begin);
    }
}
