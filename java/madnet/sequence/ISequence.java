package madnet.sequence;

public interface ISequence
{
    IBuffer buffer();

    long size();

    ISequence take(long n);
    ISequence drop(long n);

    ISequence expand(long size);
}
