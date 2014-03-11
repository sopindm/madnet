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

        while(result == null)
            result = tryPop();

        return result;
    }

    @Override
    public boolean push(Object object, long timeout) throws Exception {
        long finishTimestamp = System.currentTimeMillis() + timeout;

        do {
            if(tryPush(object)) return true;
        }while(System.currentTimeMillis() < finishTimestamp);

        return false;
    }

    @Override
    public Object pop(long timeout) throws Exception {
        long finishTimestamp = System.currentTimeMillis() + timeout;

        do {
            Object value = tryPop();
            if(value != null) return value;
        }while(System.currentTimeMillis() < finishTimestamp);

        return null;
    }
}
