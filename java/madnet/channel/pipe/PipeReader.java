package madnet.channel.pipe;

import madnet.event.Event;
import madnet.event.ISignal;
import madnet.event.SelectorSignal;
import madnet.channel.IChannel;
import madnet.channel.Result;
import madnet.channel.Events;
import madnet.range.nio.ByteRange;

public class PipeReader extends madnet.channel.Channel {
    private java.nio.channels.Pipe.SourceChannel channel = null;
    private Events events = null;

    public PipeReader(java.nio.channels.Pipe pipe)
        throws java.io.IOException {
        channel = pipe.source();
        channel.configureBlocking(false);

        ISignal onRead =
            new SelectorSignal(pipe.source(),
                               java.nio.channels.SelectionKey.OP_READ);
        onRead.attach(this);

        Event onClose = new Event();
        onClose.oneShot(true);
        onClose.attach(this);

        events = new Events(onRead, null, onClose);
    }

    @Override
    public boolean isOpen() {
        return channel.isOpen();
    }

    @Override
    public void close() throws java.io.IOException {
        events().onRead().close();
        channel.close();

        try {
            events().onClose().emit();
        }
        catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public PipeReader clone() throws CloneNotSupportedException {
        return (PipeReader)super.clone();
    }

    @Override
    public Events events() {
        return events;
    }

    @Override 
    public void register(madnet.event.ISignalSet set) throws Exception {
        set.conj(events().onRead());
        events().onRead().emit();
    }

    @Override
    public boolean tryPush(Object obj) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object tryPop() throws Exception {
        byte[] bytes = new byte[1];

        java.nio.ByteBuffer src = java.nio.ByteBuffer.wrap(bytes);
        channel.read(src);

        if(src.limit() - src.position() > 0)
            return null;

        return bytes[0];
    }

    @Override
    public Result read(IChannel ch) throws Exception {
        if(!isOpen())
            throw new java.nio.channels.ClosedChannelException();

        if(!(ch instanceof ByteRange))
            return null;

        ByteRange range = (ByteRange)ch;

        java.nio.ByteBuffer buffer = range.buffer();
        int begin = buffer.position();

        if(channel.read(buffer) < 0)
            close();

        return new Result(buffer.position() - begin,
                          buffer.position() - begin);
    }

    @Override
    public Result write(IChannel ch) {
        throw new UnsupportedOperationException();
    }
}
