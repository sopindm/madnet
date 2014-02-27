package madnet.event;

public class SelectorEvent extends SelectorSet.Event {
    public SelectorEvent(java.nio.channels.SelectableChannel channel, int op) {
        super(channel, op);
    }
}
