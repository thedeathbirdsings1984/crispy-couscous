package jd.gui.swing.jdgui.views.settings.panels.packagizer;

import java.awt.event.ActionEvent;
import java.util.List;

import jd.controlling.TaskQueue;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.WarnLevel;

import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.packagizer.PackagizerController;
import org.jdownloader.controlling.packagizer.PackagizerRule;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.translate._JDT;

public class RemoveAction extends AppAction {
    private static final long              serialVersionUID = -477419276505058907L;
    private java.util.List<PackagizerRule> selected;
    private PackagizerFilterTable          table;
    private boolean                        ignoreSelection  = false;

    public RemoveAction(PackagizerFilterTable table) {
        this.table = table;
        this.ignoreSelection = true;
        setName(_GUI.T.literally_remove());
        setIconKey(IconKey.ICON_REMOVE);
    }

    public RemoveAction(PackagizerFilterTable table, java.util.List<PackagizerRule> selected, boolean force) {
        this.table = table;
        this.selected = selected;
        setName(_GUI.T.literally_remove());
        setIconKey(IconKey.ICON_REMOVE);
    }

    protected boolean rly(String msg) {
        try {
            Dialog.getInstance().showConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI.T.literall_are_you_sure(), msg, null, null, null);
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
        final List<PackagizerRule> remove;
        if (selected != null) {
            remove = selected;
        } else {
            remove = table.getModel().getSelectedObjects();
        }
        if (remove != null && remove.size() > 0) {
            TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {
                @Override
                protected Void run() throws RuntimeException {
                    for (PackagizerRule rule : remove) {
                        if (!rule.isStaticRule()) {
                            PackagizerController.getInstance().remove(rule);
                        }
                    }
                    return null;
                }
            });
        }
    }

    @Override
    public boolean isEnabled() {
        if (ignoreSelection) {
            return super.isEnabled();
        } else if (selected != null) {
            for (PackagizerRule rule : selected) {
                if (!rule.isStaticRule()) {
                    return true;
                }
            }
        }
        return false;
    }
}
