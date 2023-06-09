package org.jdownloader.osevents;

import java.util.concurrent.atomic.AtomicBoolean;

import org.appwork.shutdown.ShutdownController;
import org.appwork.utils.event.Eventsender;
import org.jdownloader.osevents.multios.SignalEventSource;
import org.jdownloader.updatev2.ForcedShutdown;

public class OperatingSystemEventSender extends Eventsender<OperatingSystemListener, OperatingSystemEvent> implements OperatingSystemListener {
    private static final OperatingSystemEventSender INSTANCE = new OperatingSystemEventSender();

    /**
     * get the only existing instance of OperatingSystemEventSender. This is a singleton
     *
     * @return
     */
    public static OperatingSystemEventSender getInstance() {
        return OperatingSystemEventSender.INSTANCE;
    }

    private final SignalEventSource signalEventSource;

    /**
     * Create a new instance of OperatingSystemEventSender. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */
    private OperatingSystemEventSender() {
        SignalEventSource signalEventSource = null;
        try {
            signalEventSource = new SignalEventSource() {
                @Override
                public boolean onSignal(String name, int number) {
                    return OperatingSystemEventSender.this.onSignal(name, number);
                }
            };
        } catch (final Throwable e) {
            e.printStackTrace();
        }
        this.signalEventSource = signalEventSource;
    }

    protected boolean onSignal(String name, int number) {
        if ("HUP".equals(name)) {
            fireEvent(new OperatingSystemEvent(OperatingSystemEvent.Type.SIGNAL_HUP, name, number));
            onOperatingSystemTerm();
        } else if ("TERM".equals(name) || "INT".equals(name)) {
            fireEvent(new OperatingSystemEvent(OperatingSystemEvent.Type.SIGNAL_TERM, name, number));
            onOperatingSystemTerm();
        } else {
            fireEvent(new OperatingSystemEvent(OperatingSystemEvent.Type.SIGNAL, name, number));
        }
        return true;
    }

    @Override
    protected void fireEvent(OperatingSystemListener listener, OperatingSystemEvent event) {
        switch (event.getType()) {
        case SIGNAL_HUP:
        case SIGNAL_TERM:
            System.out.println("Handled Event: " + event);
            listener.onOperatingSystemTerm();
            break;
        case SIGNAL:
        default:
            System.out.println("Unhandled Event: " + event);
            break;
        }
    }

    public boolean isSignalSupported() {
        return signalEventSource != null;
    }

    public boolean setIgnoreSignal(final String signal, boolean ignore) {
        return signalEventSource != null && signalEventSource.setIgnore(signal, ignore);
    }

    private final AtomicBoolean shutdownRequested = new AtomicBoolean(false);

    @Override
    public void onOperatingSystemTerm() {
        if (shutdownRequested.compareAndSet(false, true)) {
            ShutdownController.getInstance().requestShutdown(new ForcedShutdown());
        }
    }
}