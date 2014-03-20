package madnet.channel;

import madnet.event.Event;

public abstract class ObjectWire {
    private boolean readable = true;
    private boolean writable = true;

    public boolean readable() { return readable; }
    public void closeRead() { readable = false; }

    public boolean writable() { return writable; }
    public void closeWrite() { writable = false; }

    public boolean push(Object o) {
        if(offer()) {
            commitOffer(o);
            return true;
        }

        return false;
    }

    public abstract boolean offer();
    public abstract void cancelOffer();
    public abstract void commitOffer(Object o);

    public Object pop() {
        Object obj = fetch();

        if(obj == null)
            return null;

        commitFetch();
        return obj;
    }

    public abstract Object fetch();
    public abstract void cancelFetch(Object o);
    public abstract void commitFetch();
}
