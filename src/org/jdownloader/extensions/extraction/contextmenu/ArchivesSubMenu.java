package org.jdownloader.extensions.extraction.contextmenu;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.swing.JComponent;

import jd.gui.swing.jdgui.MainTabbedPane;
import jd.gui.swing.jdgui.interfaces.View;

import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.controlling.contextmenu.MenuContainer;
import org.jdownloader.controlling.contextmenu.gui.MenuBuilder;
import org.jdownloader.extensions.ExtensionNotLoadedException;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.ArchiveValidator;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.ArchiveValidator.ArchiveValidation;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.DownloadsView;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberView;

public class ArchivesSubMenu extends MenuContainer {
    public ArchivesSubMenu() {
        setName(org.jdownloader.extensions.extraction.translate.T.T.contextmenu_main());
        setIconKey(org.jdownloader.gui.IconKey.ICON_EXTRACT);
    }

    private SelectionInfo<?, ?> _getSelection() {
        final View view = MainTabbedPane.getInstance().getSelectedView();
        if (view instanceof DownloadsView) {
            return DownloadsTable.getInstance().getSelectionInfo();
        } else if (view instanceof LinkGrabberView) {
            return LinkGrabberTable.getInstance().getSelectionInfo();
        }
        return null;
    }

    @Override
    public JComponent addTo(JComponent root, MenuBuilder menuBuilder) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException, SecurityException, ExtensionNotLoadedException {
        final JComponent ret = super.addTo(root, menuBuilder);
        ret.setEnabled(false);
        final ArchiveValidation result = ArchiveValidator.validate(_getSelection(), true);
        result.executeWhenReached(new Runnable() {
            @Override
            public void run() {
                final List<Archive> archives = result.getArchives();
                if (archives != null && archives.size() > 0) {
                    new EDTRunner() {
                        @Override
                        protected void runInEDT() {
                            ret.setEnabled(true);
                        }
                    };
                }
            }
        });
        return ret;
    }
}
