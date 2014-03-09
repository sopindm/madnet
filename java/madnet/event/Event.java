package madnet.event;

import java.util.Iterator;
import java.util.HashSet;
import java.util.LinkedList;

public class Event  extends EventHandler implements IEvent {
    @Override
    public void close() {
        Iterator<IEventHandler> it = handlers.iterator();
        while(it.hasNext()) disj(it.next());

        attachment = null;

        super.close();
    }

    boolean oneShot = false;
    public boolean oneShot() { return oneShot; }
    public void oneShot(boolean value) { oneShot = value; }

    boolean inIteration = false;

    @Override
    public void emit() {
        handle();
    }

    public void handle() {
        handle(attachment);
    }

    public void handle(Object source) {
        if(inIteration)
            throw new UnsupportedOperationException("Cannot emit emitting event");

        try {
            inIteration = true;
            for(IEventHandler handler : handlers)
                handler.call(this, source);
        }
        finally {
            inIteration = false;
        }

        updateHandlers();
        
        if(oneShot)
            close();
    }

    HashSet<IEventHandler> handlers = new HashSet<IEventHandler>();
    LinkedList<IEventHandler> adding = new LinkedList<IEventHandler>();
    LinkedList<IEventHandler> removing = new LinkedList<IEventHandler>();

    private void updateHandlers() {
        for(IEventHandler h : adding) conjImpl(h);
        adding.clear();

        for(IEventHandler h :removing) disjImpl(h);
        removing.clear();
    }

    @Override
    public HashSet<IEventHandler> handlers() { return handlers; }

    private void conjImpl(IEventHandler handler) {
        handlers.add(handler);
        handler.subscribe(this);
    }

    @Override
    public void conj(IEventHandler handler) {
        if(inIteration)
            adding.add(handler);
        else 
            conjImpl(handler);

        handler.subscribe(this);
    }

    private void disjImpl(IEventHandler handler) {
        handlers.remove(handler);
    }

    @Override
    public void disj(IEventHandler handler) {
        if(inIteration)
            removing.add(handler);
        else
            disjImpl(handler);

        handler.unsubscribe(this);
    }

    Object attachment = null;

    @Override
    public Object attachment() { return attachment; }
    @Override
    public void attach(Object attachment) { this.attachment = attachment; }
}
