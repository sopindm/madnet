package madnet.channel.tcp;

import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import madnet.channel.SelectableChannel;
import madnet.channel.ReadableChannel;
import madnet.channel.WritableChannel;
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
    
    @Override
    public Object tryPop() throws Exception {
        if(!connected) {
            connected = channel.finishConnect();
        }

        if(!connected)
            return null;

        ArrayList<SelectableChannel> ret = new ArrayList<SelectableChannel>(2);
        ret.add(new ReadableChannel<SocketChannel>(channel));
        ret.add(new WritableChannel<SocketChannel>(channel));

        return ret;
    }
}
