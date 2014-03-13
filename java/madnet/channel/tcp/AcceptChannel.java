package madnet.channel.tcp;

import java.nio.channels.SocketChannel;
import java.nio.channels.ServerSocketChannel;
import madnet.channel.IChannel;
import madnet.channel.Result;
import madnet.channel.ReadableChannel;
import madnet.channel.WritableChannel;
import madnet.channel.SelectableChannel;
import madnet.event.ISignalSet;

public class AcceptChannel extends SelectableChannel<ServerSocketChannel>
{
    public AcceptChannel(ServerSocketChannel ch) throws Exception {
        super(ch);
    }

    @Override
    public void register(ISignalSet set) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object tryPop() throws Exception {
        SocketChannel socket = channel.accept();
        return new Socket(socket);
    }

    Socket preAccepted = null;

    @Override
    public Result read(IChannel ch) throws Exception {
        if(preAccepted == null)
            preAccepted = new Socket(channel.accept());

        if(preAccepted == null)
            return null;

        try {
            if(ch.tryPush(preAccepted)) return Result.ONE;
        }
        catch(IllegalArgumentException e) {
            return null;
        }

        return Result.ZERO;
    }
}
