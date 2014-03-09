package madnet.event;

import java.util.Iterator;
import java.util.HashSet;
import java.util.LinkedList;

public class Event  extends EventHandler implements IEvent {
    @Override
    public void close() {
        Iterator<IEventHandler> it = handlers.iterator();
        while(it.hasNext()) popHandler(it.next());

        super.close();
    }

    boolean oneShot = false;
    public boolean oneShot() { return oneShot; }
    public void oneShot(boolean value) { oneShot = value; }

    boolean inIteration = false;

    @Override
    public void emit(Object source) {
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
        for(IEventHandler h : adding) pushHandlerImpl(h);
        adding.clear();

        for(IEventHandler h :removing) popHandlerImpl(h);
        removing.clear();
    }

    @Override
    public HashSet<IEventHandler> handlers() { return handlers; }

    private void pushHandlerImpl(IEventHandler handler) {
        handlers.add(handler);
        handler.subscribe(this);
    }

    @Override
    public void pushHandler(IEventHandler handler) {
        if(inIteration)
            adding.add(handler);
        else 
            pushHandlerImpl(handler);

        handler.subscribe(this);
    }

    private void popHandlerImpl(IEventHandler handler) {
        handlers.remove(handler);
    }

    @Override
    public void popHandler(IEventHandler handler) {
        if(inIteration)
            removing.add(handler);
        else
            popHandlerImpl(handler);

        handler.unsubscribe(this);
    }
}
