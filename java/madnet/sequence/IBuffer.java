package madnet.sequence;

public interface IBuffer
{
    ASequence sequence(int offset, int size, int limit);
    int size();
}