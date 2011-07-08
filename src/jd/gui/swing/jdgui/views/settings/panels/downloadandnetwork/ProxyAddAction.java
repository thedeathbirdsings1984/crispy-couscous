package jd.gui.swing.jdgui.views.settings.panels.downloadandnetwork;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import jd.controlling.IOEQ;
import jd.controlling.JDLogger;
import jd.controlling.proxy.ProxyController;
import jd.gui.swing.dialog.ProxyDialog;

import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.images.NewTheme;
import org.jdownloader.translate._JDT;

public class ProxyAddAction extends AbstractAction {

    public ProxyAddAction(ProxyTable table) {
        super("Add new Proxy");
        this.putValue(NAME, _JDT._.basics_add());
        this.putValue(AbstractAction.SMALL_ICON, NewTheme.I().getIcon("add", 20));
    }

    public ProxyAddAction() {
        super("Add new Proxy");
        this.putValue(NAME, _JDT._.basics_add());
        this.putValue(AbstractAction.SMALL_ICON, NewTheme.I().getIcon("add", 16));
    }

    /**
     * 
     */
    private static final long serialVersionUID = -197136045388327528L;

    public void actionPerformed(ActionEvent e) {
        ProxyDialog proxyDialog = new ProxyDialog();
        try {
            final HTTPProxy proxy = Dialog.getInstance().showDialog(proxyDialog);
            if (proxy == null) return;
            IOEQ.add(new Runnable() {

                public void run() {
                    ProxyController.getInstance().addProxy(proxy);
                }
            });
        } catch (final DialogClosedException e1) {
        } catch (final DialogCanceledException e1) {
        } catch (final Throwable e1) {
            JDLogger.exception(e1);
        }
    }
}
