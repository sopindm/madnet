package madnet.event;

import java.util.Set;
import java.util.WeakHashMap;
import java.util.Collections;

public class AEvent implements IEvent {
    Set<IEventHandler> handlers = Collections.newSetFromMap(new WeakHashMap<IEventHandler, Boolean>());

    public Set<IEventHandler> handlers() { return handlers; }

    @Override
    public void pushHandler(IEventHandler handler) { handlers.add(handler); }
    @Override
    public void popHandler(IEventHandler handler) { handlers.remove(handler); }
}
