package madnet.channel;

import madnet.channel.Result;

public abstract class Channel implements IChannel {
    @Override
    public abstract Result read(IChannel ch) throws Exception;
    @Override
    public abstract Result write(IChannel ch) throws Exception;
}
