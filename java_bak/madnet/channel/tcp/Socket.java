package madnet.channel.tcp;

import java.nio.channels.SocketChannel;
import madnet.channel.ReadableChannel;
import madnet.channel.WritableChannel;

public class Socket implements madnet.channel.ISocket {
    SocketChannel ch = null;

    boolean readerClosed = false;
    boolean writerClosed = false;

    public class Reader extends ReadableChannel<SocketChannel> {
        Reader() throws Exception {
            super(ch);
        }

        @Override
        public void close() throws java.io.IOException {
            readerClosed = true;
            if(!writerClosed) channel = null;

            super.close();
        }
    }

    public class Writer extends WritableChannel<SocketChannel> {
        Writer() throws Exception {
            super(ch);
        }

        @Override
        public void close() throws java.io.IOException {
            writerClosed = true;
            if(!readerClosed) channel = null;

            super.close();
        }
    }

    Reader reader = null;
    Writer writer = null;

    public Socket(SocketChannel channel) throws Exception {
        this.ch = channel;

        reader = new Reader();
        writer = new Writer();
    }

    @Override
    public void close() throws java.io.IOException {
        ch.close();
    }

    @Override
    public ReadableChannel<SocketChannel> reader() throws Exception {
        return reader;
    }

    @Override
    public WritableChannel<SocketChannel> writer() throws Exception {
        return writer;
    }
}
