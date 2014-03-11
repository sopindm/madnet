package madnet.channel;

public interface IQueue {
    public void push(Object object) throws Exception;
    public Object pop() throws Exception;

    public boolean push(Object object, long timeout) throws Exception;
    public Object pop(long timeout) throws Exception;

    public boolean tryPush(Object object) throws Exception;
    public Object tryPop() throws Exception;
}
