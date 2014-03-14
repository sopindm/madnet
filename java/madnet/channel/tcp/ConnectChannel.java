package madnet.channel.tcp;

import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import madnet.channel.IChannel;
import madnet.channel.SelectableChannel;
import madnet.channel.ReadableChannel;
import madnet.channel.WritableChannel;
import madnet.channel.Result;
import madnet.event.ISignalSet;
import madnet.event.SelectorSignal;

public class ConnectChannel extends SelectableChannel<SocketChannel>
{
    boolean connected = false;

    public ConnectChannel(SocketChannel ch, SocketAddress remote)
        throws Exception {
        super(ch);

        events.onWrite(new SelectorSignal(ch, java.nio.channels.SelectionKey.OP_CONNECT));

        connected = ch.connect(remote);
    }

    @Override
    public boolean isOpen() {
        if(channel == null) return false;
        return super.isOpen();
    }

    @Override
    public void register(ISignalSet set) throws Exception {
        set.conj(events.onWrite());
        events.onWrite().emit();
    }
    
    private boolean tryConnect() throws java.io.IOException {
        if(connected)
            return true;

        connected = channel.finishConnect();
        return connected;
    }

    @Override
    public Object tryPop() throws Exception {
        if(!tryConnect())
            return null;

        Socket result = new Socket(channel);
        channel = null;
        close();

        return result;
    }

    @Override
    public Result read(IChannel ch) throws Exception {
        if(!tryConnect())
            return Result.ZERO;

        try {
            if(ch.tryPush(new Socket(channel))) {
                channel = null;
                close();

                return Result.ONE;
            }
        }
        catch(IllegalArgumentException e) {
            return null;
        }

        return Result.ZERO;
    }
}
