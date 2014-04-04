package madnet.event;

public class SelectorSignal extends SelectorSet.Signal {
    public SelectorSignal(java.nio.channels.SelectableChannel channel, int op) {
        super(channel, op);
    }
}
