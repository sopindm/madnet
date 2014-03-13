package madnet.channel.tcp;

import java.nio.channels.SocketChannel;
import madnet.channel.ReadableChannel;
import madnet.channel.WritableChannel;

public class Socket implements madnet.channel.ISocket {
    SocketChannel channel = null;

    public Socket(SocketChannel channel) {
        this.channel = channel;
    }

    @Override
    public void close() throws java.io.IOException {
        channel.close();
    }

    @Override
    public ReadableChannel<SocketChannel> reader() throws Exception {
        return new ReadableChannel<SocketChannel>(channel);
    }

    @Override
    public WritableChannel<SocketChannel> writer() throws Exception {
        return new WritableChannel<SocketChannel>(channel);
    }
}
