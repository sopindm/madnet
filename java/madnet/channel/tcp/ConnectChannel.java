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

public class ConnectChannel extends SelectableChannel<SocketChannel>
{
    boolean connected;

    public ConnectChannel(SocketChannel ch, SocketAddress remote)
        throws Exception {
        super(ch);

        connected = ch.connect(remote);
    }

    @Override
    public void register(ISignalSet set) {
        throw new UnsupportedOperationException();
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

        return new Socket(channel);
    }

    @Override
    public Result read(IChannel ch) throws Exception {
        if(!tryConnect())
            return Result.ZERO;

        try {
            if(ch.tryPush(new Socket(channel)))
                return Result.ONE;
        }
        catch(IllegalArgumentException e) {
            return null;
        }

        return Result.ZERO;
    }
}
