package madnet.util;

import java.util.Iterator;

public class MultiIterable<T> implements Iterable<T> {
    Iterable<? extends T> first;
    Iterable<Iterable<? extends T>> rest;

    @SafeVarargs
    public MultiIterable(Iterable<? extends T> first, Iterable<? extends T>... rest) {
        this.first = first;
        this.rest = java.util.Arrays.asList(rest);
    }

    @Override
    public Iterator<T> iterator() {
        return new MultiIterator<T>(first.iterator(), rest.iterator());
    }
}
