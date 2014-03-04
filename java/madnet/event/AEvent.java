package madnet.event;

import java.util.Set;
import java.util.HashSet;

public class AEvent implements IEvent {
    HashSet<IEventHandler> handlers = new HashSet<IEventHandler>();

    public Set<IEventHandler> handlers() { return handlers; }

    @Override
    public void pushHandler(IEventHandler handler) { handlers.add(handler); }
    @Override
    public void popHandler(IEventHandler handler) { handlers.remove(handler); }
}
