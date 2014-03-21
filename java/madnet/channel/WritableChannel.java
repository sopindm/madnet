package madnet.channel;

import java.nio.channels.WritableByteChannel;
import madnet.event.Event;
import madnet.event.ISignal;
import madnet.event.SelectorSignal;
import madnet.channel.IChannel;
import madnet.channel.Result;
import madnet.channel.Events;
import madnet.range.nio.ByteRange;

public class WritableChannel<T extends java.nio.channels.SelectableChannel &
                                       WritableByteChannel> 
    extends SelectableChannel<T> {
    public WritableChannel(T ch) throws java.io.IOException {
        super(ch);

        ISignal onWrite = 
            new SelectorSignal(ch, 
                               java.nio.channels.SelectionKey.OP_WRITE);
        onWrite.attach(this);
        events.onWrite(onWrite);
    }

    @Override
    public WritableChannel clone() throws CloneNotSupportedException {
        return (WritableChannel)super.clone();
    }

    @Override 
    public void register(madnet.event.ISignalSet set) throws Exception {
        set.conj(events.onWrite());
        events.onWrite().start();
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
