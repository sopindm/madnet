package madnet.channel.tcp;

import java.nio.channels.SocketChannel;
import java.nio.channels.ServerSocketChannel;
import java.util.ArrayList;
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

        ArrayList<SelectableChannel> ret = new ArrayList<SelectableChannel>(2);
        ret.add(new ReadableChannel<SocketChannel>(socket));
        ret.add(new WritableChannel<SocketChannel>(socket));

        return ret;
    }
}
