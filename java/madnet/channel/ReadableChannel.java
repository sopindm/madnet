package madnet.channel;

import java.nio.channels.ReadableByteChannel;
import madnet.event.Event;
import madnet.event.ISignal;
import madnet.event.SelectorSignal;
import madnet.channel.IChannel;
import madnet.channel.Result;
import madnet.channel.Events;
import madnet.range.nio.ByteRange;

public class ReadableChannel<T extends java.nio.channels.SelectableChannel &
                                       ReadableByteChannel> 
    extends SelectableChannel<T> {
    public ReadableChannel(T ch) throws java.io.IOException {
        super(ch);

        ISignal onRead = 
            new SelectorSignal(ch, 
                               java.nio.channels.SelectionKey.OP_READ);
        onRead.attach(this);
        events.onRead(onRead);
    }

    @Override
    public ReadableChannel clone() throws CloneNotSupportedException {
        return (ReadableChannel)super.clone();
    }

    @Override 
    public void register(madnet.event.ISignalSet set) throws Exception {
        set.conj(events.onRead());
        events.onRead().emit();
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
}
