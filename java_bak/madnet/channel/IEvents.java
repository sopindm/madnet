package madnet.channel;

import madnet.event.IEvent;
import madnet.event.ISignal;

public interface IEvents {
    public ISignal onRead();
    public ISignal onWrite();
    public IEvent onClose();
}
