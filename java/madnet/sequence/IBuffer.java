package madnet.sequence;

public interface IBuffer
{
    ISequence sequence(long offset, long size);
    long size();
}