package madnet.channel;

import madnet.event.Event;
import madnet.event.ISignal;
import madnet.event.SelectorSignal;
import madnet.channel.IChannel;
import madnet.channel.Result;
import madnet.channel.Events;
import madnet.range.nio.ByteRange;
import java.nio.channels.Pipe;

public class PipeReader
    extends madnet.channel.ReadableChannel<Pipe.SourceChannel>{
    public PipeReader(Pipe pipe) throws java.io.IOException {
        super(pipe.source());
    }
}
