package madnet.event;

import java.util.HashSet;

public class Event  extends EventHandler implements IEvent {
    @Override
    public void close() {
        handlers.clear();
        super.close();
    }

    @Override
    public void call(Object emitter, Object source) {
        super.call(emitter, source);
    }

    @Override
    public void emit(Object source) {
        for(IEventHandler handler : handlers)
            handler.call(this, source);
    }

    HashSet<IEventHandler> handlers = new HashSet<IEventHandler>();

    @Override
    public HashSet<IEventHandler> handlers() { return handlers; }

    @Override
    public void pushHandler(IEventHandler handler) {
        handlers.add(handler);
        handler.subscribe(this);
    }

    @Override
    public void popHandler(IEventHandler handler) {
        handlers.remove(handler);
        handler.unsubscribe(this);
    }
}
