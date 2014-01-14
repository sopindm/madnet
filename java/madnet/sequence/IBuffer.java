package madnet.sequence;

public interface IBuffer
{
    ISequence sequence(int offset, int size);
    int size();
}