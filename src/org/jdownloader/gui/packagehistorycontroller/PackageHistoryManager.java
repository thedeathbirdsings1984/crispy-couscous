package org.jdownloader.gui.packagehistorycontroller;

import java.util.ArrayList;
import java.util.List;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.ConfigEvent;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.StringUtils;
import org.appwork.utils.event.EventSuppressor;
import org.jdownloader.settings.staticreferences.CFG_LINKGRABBER;

public class PackageHistoryManager extends HistoryManager<PackageHistoryEntry> implements GenericConfigEventListener<Object> {
    private static final PackageHistoryManager INSTANCE = new PackageHistoryManager();

    /**
     * get the only existing instance of PackageHistoryManager. This is a singleton
     *
     * @return
     */
    public static PackageHistoryManager getInstance() {
        return PackageHistoryManager.INSTANCE;
    }

    /**
     * Create a new instance of PackageHistoryManager. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */
    private PackageHistoryManager() {
        super(CFG_LINKGRABBER.CFG.getPackageNameHistory());
        CFG_LINKGRABBER.PACKAGE_NAME_HISTORY.getEventSender().addListener(this);
    }

    @Override
    protected int getMaxLength() {
        return CFG_LINKGRABBER.CFG.getPackageNameHistoryLength();
    }

    @Override
    protected boolean isValid(String input) {
        return !StringUtils.isEmpty(input) && !input.contains("<jd:");
    }

    @Override
    protected void save(List<PackageHistoryEntry> list) {
        final Thread thread = Thread.currentThread();
        final EventSuppressor<ConfigEvent> eventSuppressor = new EventSuppressor<ConfigEvent>() {
            @Override
            public boolean suppressEvent(ConfigEvent eventType) {
                return Thread.currentThread() == thread;
            }
        };
        CFG_LINKGRABBER.PACKAGE_NAME_HISTORY.getEventSender().addEventSuppressor(eventSuppressor);
        try {
            CFG_LINKGRABBER.CFG.setPackageNameHistory(list);
        } finally {
            CFG_LINKGRABBER.PACKAGE_NAME_HISTORY.getEventSender().removeEventSuppressor(eventSuppressor);
        }
    }

    @Override
    protected PackageHistoryEntry createNew(String name) {
        return new PackageHistoryEntry(name);
    }

    public List<PackageHistoryEntry> list(PackageHistoryEntry packageHistoryEntry) {
        final List<PackageHistoryEntry> list = list();
        final ArrayList<PackageHistoryEntry> ret = new ArrayList<PackageHistoryEntry>();
        ret.add(packageHistoryEntry);
        for (final PackageHistoryEntry phe : list) {
            if (!phe.getName().equals(packageHistoryEntry.getName())) {
                ret.add(phe);
            }
        }
        return ret;
    }

    @Override
    public void onConfigValueModified(KeyHandler<Object> keyHandler, Object newValue) {
        if (newValue == null) {
            clear();
        } else if (newValue instanceof List) {
            final List<Object> list = (List<Object>) newValue;
            if (list.size() == 0) {
                clear();
            } else {
                synchronized (this) {
                    clear();
                    for (int i = list.size() - 1; i >= 0; i--) {
                        final Object item = list.get(i);
                        if (item != null && item instanceof PackageHistoryEntry) {
                            add(((PackageHistoryEntry) item).getName());
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onConfigValidatorError(KeyHandler<Object> keyHandler, Object invalidValue, ValidationException validateException) {
    }
}
