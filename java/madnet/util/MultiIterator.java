package madnet.util;

import java.util.Iterator;

public class MultiIterator<T> implements Iterator<T> {
    Iterator<? extends T> first;
    Iterator<Iterable<? extends T>> rest;

    MultiIterator(Iterator<? extends T> first, Iterator<Iterable<? extends T>> rest) {
        this.first = first;
        this.rest = rest;
    }

    @Override
    public boolean hasNext() {
        if(first.hasNext())
            return true;

        if(!rest.hasNext())
            return false;

        first = rest.next().iterator();

        return hasNext();
    }

    @Override
    public T next() {
        if(first.hasNext())
            return first.next();

        first = rest.next().iterator();
        return next();
    }

    @Override
    public void remove() {
        first.remove();
    }
}

