package madnet.channel;

public interface ObjectWire {
    boolean push(Object o);

    boolean offer();
    void cancelOffer();
    void commitOffer(Object o);

    Object pop();

    Object fetch();
    void cancelFetch(Object o);
    void commitFetch();
}
