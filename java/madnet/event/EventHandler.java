package madnet.event;

import java.util.Iterator;
import java.util.HashSet;

public abstract class EventHandler implements IEventHandler {
    @Override
    public void close() {
        Iterator<IEvent> events = emitters.iterator();

        while(events.hasNext()) {
            IEvent e = events.next();
            unsubscribe(e);

            e.popHandler(this);
        }
    }

    boolean oneShot = false;
    public boolean oneShot() { return oneShot; }
    public void oneShot(boolean value) { oneShot = value; }

    @Override
    public void call(Object emitter, Object source) {
        if(oneShot())
            close();
    }

    HashSet<IEvent> emitters = new HashSet<IEvent>();
    
    @Override
    public void subscribe(IEvent event) { emitters.add(event); }
    @Override
    public void unsubscribe(IEvent event) { emitters.remove(event); }
}
