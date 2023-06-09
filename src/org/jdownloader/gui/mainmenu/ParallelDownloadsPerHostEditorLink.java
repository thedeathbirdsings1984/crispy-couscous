package org.jdownloader.gui.mainmenu;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jd.gui.swing.jdgui.menu.ParallelDownloadsPerHostEditor;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtSpinner;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.contextmenu.ActionData;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.controlling.contextmenu.MenuLink;
import org.jdownloader.controlling.contextmenu.gui.MenuBuilder;
import org.jdownloader.extensions.ExtensionNotLoadedException;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.MenuManagerDownloadTabBottomBar;
import org.jdownloader.updatev2.gui.LAFOptions;

public class ParallelDownloadsPerHostEditorLink extends MenuItemData implements MenuLink {
    public ParallelDownloadsPerHostEditorLink() {
        super();
        setName(_GUI.T.ParalellDownloadsEditor_ParallelDownloadsPerHostEditor_());
        setIconKey(IconKey.ICON_BATCH);
        //
    }

    @Override
    public List<AppAction> createActionsToLink() {
        return null;
    }

    @Override
    public JComponent createSettingsPanel() {
        ActionData ad = getActionData();
        final ActionData actionData = ad;
        MigPanel p = new MigPanel("ins 0,wrap 2", "[grow,fill][]", "[]");
        SwingUtils.setOpaque(p, false);
        p.add(new JLabel(_GUI.T.MenuEditors_editorwidth()));
        int width = _getPreferedEditorWidth();
        final ExtSpinner spinner = new ExtSpinner(new SpinnerNumberModel(width, -1, 10000, 1));
        spinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                actionData.putSetup("width", ((Number) spinner.getValue()).intValue());
            }
        });
        p.add(spinner);
        return p;
    }

    protected int _getPreferedEditorWidth() {
        int width = -1;
        try {
            width = ((Number) getActionData().fetchSetup("width")).intValue();
        } catch (Throwable e) {
        }
        return width;
    }

    @Override
    public JComponent createItem(MenuBuilder menuBuilder) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException, SecurityException, ExtensionNotLoadedException {
        return new ParallelDownloadsPerHostEditor() {
            @Override
            protected int getEditorWidth() {
                int ret = _getPreferedEditorWidth();
                if (ret < 10) {
                    ret = super.getEditorWidth();
                }
                return ret;
            }

            @Override
            protected String getInsetsString() {
                if (_getRoot()._getOwner() instanceof MenuManagerDownloadTabBottomBar) {
                    return "0";
                }
                return super.getInsetsString();
            }

            protected int getComponentHeight() {
                if (_getRoot()._getOwner() instanceof MenuManagerDownloadTabBottomBar) {
                    return 24;
                }
                return 22;
            }

            @Override
            protected int getIconTextGap() {
                return LAFOptions.getInstance().getExtension().customizeMenuItemIconTextGap();
            }
        };
    }
}
