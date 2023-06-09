package jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter;

import java.awt.event.ActionEvent;
import java.util.List;

import jd.controlling.TaskQueue;
import jd.gui.UserIO;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.WarnLevel;

import org.appwork.swing.exttable.ExtTable;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.filter.LinkFilterController;
import org.jdownloader.controlling.filter.LinkgrabberFilterRule;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.translate._JDT;

public class RemoveAction extends AppAction {
    private static final long                     serialVersionUID = -477419276505058907L;
    private java.util.List<LinkgrabberFilterRule> selected;
    private boolean                               ignoreSelection  = false;
    private AbstractFilterTable                   table;
    private LinkgrabberFilter                     linkgrabberFilter;

    public RemoveAction(LinkgrabberFilter linkgrabberFilter) {
        this.linkgrabberFilter = linkgrabberFilter;
        this.ignoreSelection = true;
        setName(_GUI.T.literally_remove());
        setIconKey(IconKey.ICON_REMOVE);
    }

    public RemoveAction(AbstractFilterTable table, java.util.List<LinkgrabberFilterRule> selected, boolean force) {
        this.table = table;
        this.selected = selected;
        setName(_GUI.T.literally_remove());
        setIconKey(IconKey.ICON_REMOVE);
    }

    protected boolean rly(String msg) {
        try {
            Dialog.getInstance().showConfirmDialog(UserIO.DONT_SHOW_AGAIN | UserIO.DONT_SHOW_AGAIN_IGNORES_CANCEL, _GUI.T.literall_are_you_sure(), msg, null, null, null);
            return true;
        } catch (DialogClosedException e) {
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) {
            return;
        } else if (JDGui.bugme(WarnLevel.NORMAL)) {
            if (!rly(_JDT.T.RemoveAction_actionPerformed_rly_msg())) {
                return;
            }
        }
        final List<LinkgrabberFilterRule> remove;
        if (selected != null) {
            remove = selected;
        } else {
            remove = getTable().getModel().getSelectedObjects();
        }
        if (remove != null && remove.size() > 0) {
            TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {
                @Override
                protected Void run() throws RuntimeException {
                    for (final LinkgrabberFilterRule rule : remove) {
                        if (!rule.isStaticRule()) {
                            LinkFilterController.getInstance().remove(rule);
                        }
                    }
                    return null;
                }
            });
        }
    }

    private ExtTable<LinkgrabberFilterRule> getTable() {
        if (table != null) {
            return table;
        } else {
            return linkgrabberFilter.getTable();
        }
    }

    @Override
    public boolean isEnabled() {
        if (ignoreSelection) {
            return super.isEnabled();
        } else if (selected != null) {
            for (LinkgrabberFilterRule rule : selected) {
                if (!rule.isStaticRule()) {
                    return true;
                }
            }
        }
        return false;
    }
}
