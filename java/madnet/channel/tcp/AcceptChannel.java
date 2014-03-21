package madnet.channel.tcp;

import java.nio.channels.SocketChannel;
import java.nio.channels.ServerSocketChannel;
import madnet.channel.IChannel;
import madnet.channel.Result;
import madnet.channel.ReadableChannel;
import madnet.channel.WritableChannel;
import madnet.channel.SelectableChannel;
import madnet.event.ISignalSet;
import madnet.event.SelectorSignal;

public class AcceptChannel extends SelectableChannel<ServerSocketChannel>
{
    public AcceptChannel(ServerSocketChannel ch) throws Exception {
        super(ch);

        events.onWrite(new SelectorSignal(ch, java.nio.channels.SelectionKey.OP_ACCEPT));
    }

    @Override
    public void register(ISignalSet set) throws Exception {
        set.conj(events.onWrite());
        events.onWrite().start();
    }

    Socket preAccepted = null;

    private boolean tryFetch() throws Exception {
        if(preAccepted != null)
            return true;

        SocketChannel accepted = channel.accept();

        if(accepted != null)
            preAccepted = new Socket(accepted);

        return accepted != null;
    }

    @Override
    public Object tryPop() throws Exception {
        if(!tryFetch())
            return null;

        Socket result = preAccepted;
        preAccepted = null;

        return result;
    }


    @Override
    public Result read(IChannel ch) throws Exception {
        if(!tryFetch())
            return Result.ZERO;

        try {
            if(ch.tryPush(preAccepted)) {
                preAccepted = null;
                return Result.ONE.add(read(ch));
            }
        }
        catch(IllegalArgumentException e) {
            return null;
        }

        return Result.ZERO;
    }
}
