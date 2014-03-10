package madnet.event;

import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;

public class EventHandler implements IEventHandler {
    @Override
    public void close() throws java.io.IOException {
        Iterator<IEvent> events = emitters.iterator();

        while(events.hasNext()) {
            IEvent e = events.next();
            events.remove();

            e.disj(this);
        }
    }

    @Override
    public void call(Object emitter, Object source) {
    }

    HashSet<IEvent> emitters = new HashSet<IEvent>();

    public Set<IEvent> emitters() { return emitters; };
    
    @Override
    public void subscribe(IEvent event) { emitters.add(event); }
    @Override
    public void unsubscribe(IEvent event) { emitters.remove(event); }
}
