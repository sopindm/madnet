package madnet.event;

public interface ISet<T> {
    public void conj(T value) throws Exception;
    public void disj(T value) throws Exception;
}
