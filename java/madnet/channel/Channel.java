package madnet.channel;

public abstract class Channel implements IChannel {
    @Override
    public Channel clone() throws CloneNotSupportedException {
        return (Channel)super.clone();
    }

    @Override
    public void push(Object object) throws Exception {
        while(!tryPush(object));
    }

    @Override
    public Object pop() throws Exception {
        Object result = tryPop();

        while(result == null && !Thread.currentThread().isInterrupted())
            result = tryPop();

        if(Thread.interrupted())
            throw new InterruptedException();

        return result;
    }

    @Override
    public Object peek() throws Exception {
        Object result = tryPeek();

        while(result == null)
            result = tryPeek();

        return result;
    }

    @Override
    public boolean push(Object object, long timeout) throws Exception {
        long finishTimestamp = System.currentTimeMillis() + timeout;

        do {
            if(tryPush(object)) return true;
        }
        while(System.currentTimeMillis() < finishTimestamp);

        return false;
    }

    @Override
    public Object pop(long timeout) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object peek(long timeout) throws Exception {
        throw new UnsupportedOperationException();
    }
}
