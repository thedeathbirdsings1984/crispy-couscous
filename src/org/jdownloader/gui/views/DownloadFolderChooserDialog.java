package org.jdownloader.gui.views;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import jd.gui.swing.jdgui.views.settings.components.FolderChooser;
import net.miginfocom.swing.MigLayout;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtTextField;
import org.appwork.utils.StringUtils;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.ExtFileChooserDialog;
import org.appwork.utils.swing.dialog.FileChooserSelectionMode;
import org.appwork.utils.swing.dialog.dimensor.RememberLastDialogDimension;
import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.packagizer.PackagizerController;
import org.jdownloader.gui.packagehistorycontroller.DownloadPathHistoryManager;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.GraphicalUserInterfaceSettings.DownloadFolderChooserDialogSubfolder;
import org.jdownloader.settings.staticreferences.CFG_GENERAL;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class DownloadFolderChooserDialog extends ExtFileChooserDialog {
    private javax.swing.JCheckBox cbPackage;
    private final File            path;
    private boolean               packageSubFolderSelectionVisible = false;
    private boolean               subfolderFlag                    = false;

    /**
     * @param flag
     * @param title
     * @param okOption
     * @param cancelOption
     */
    public DownloadFolderChooserDialog(File path, String title, String okOption, String cancelOption) {
        super(0, title, okOption, cancelOption);
        StackTraceElement[] st = new Exception().getStackTrace();
        int i = 1;
        String id = "DownloadFolderChooserDialog-";
        try {
            while (i < st.length) {
                StackTraceElement ste = new Exception().getStackTrace()[i];
                if (!ste.getClassName().contains(DownloadFolderChooserDialog.class.getName())) {
                    id += ste.getClassName();
                    break;
                }
                i++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        setDimensor(new RememberLastDialogDimension(id));
        DownloadFolderChooserDialogSubfolder subfolderSettings = CFG_GUI.CFG.getDownloadFolderChooserDialogSubfolder();
        if (subfolderSettings == null) {
            subfolderSettings = DownloadFolderChooserDialogSubfolder.AUTO;
        }
        if (path != null) {
            if (path.getAbsolutePath().endsWith(PackagizerController.PACKAGETAG)) {
                this.subfolderFlag = true;
                path = path.getParentFile();
            }
            setPreSelection(path);
        }
        this.path = path;
        switch (subfolderSettings) {
        case ENABLED:
            this.subfolderFlag = true;
            break;
        case DISABLED:
            this.subfolderFlag = false;
            break;
        case AUTO:
        default:
            break;
        }
        setView(CFG_GUI.CFG.getFileChooserView());
    }

    @Override
    public void dispose() {
        try {
            if (isInitialized()) {
                final File[] dest = createReturnValue();
                if (dest != null && dest.length > 0) {
                    dest[0] = FolderChooser.checkPath(dest[0], null);
                    if (dest[0] != null) {
                        DownloadPathHistoryManager.getInstance().add(dest[0].getAbsolutePath());
                    }
                }
            }
        } finally {
            super.dispose();
        }
    }

    @Override
    protected File[] createReturnValue() {
        CFG_GUI.CFG.setFileChooserView(getView());
        if (isMultiSelection()) {
            File[] files = fc.getSelectedFiles();
            return files;
        } else {
            File f = fc.getSelectedFile();
            if (f == null) {
                String path = getText();
                if (!StringUtils.isEmpty(path) && isAllowedPath(path)) {
                    // if (path.start)
                    f = new File(path);
                    if (isSambaFolder(f)) {
                        return null;
                    }
                } else {
                    return null;
                }
            }
            if (cbPackage != null) {
                if (cbPackage.isSelected()) {
                    // subfolder enabled
                    if (f.getAbsolutePath().endsWith(PackagizerController.PACKAGETAG)) {
                        return new File[] { f };
                    } else {
                        // add subfolder
                        return new File[] { new File(f, PackagizerController.PACKAGETAG) };
                    }
                } else {
                    // subfolder disabled
                    if (f.getAbsolutePath().endsWith(PackagizerController.PACKAGETAG)) {
                        // remove subfolder
                        return new File[] { f.getParentFile() };
                    } else {
                        return new File[] { f };
                    }
                }
            } else {
                return new File[] { f };
            }
        }
    }

    @Override
    public JComponent layoutDialogContent() {
        final MigPanel ret;
        if (path != null) {
            ret = new MigPanel("ins 0,wrap 2", "[grow,fill][]", "[][][][grow,fill]");
            final ExtTextField lbl = new ExtTextField();
            lbl.setText(_GUI.T.OpenDownloadFolderAction_layoutDialogContent_current_(path.getAbsolutePath()));
            lbl.setEditable(false);
            if (CrossSystem.isOpenFileSupported()) {
                ret.add(lbl);
                ret.add(new JButton(new AppAction() {
                    {
                        setName(_GUI.T.OpenDownloadFolderAction_actionPerformed_button_());
                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        CrossSystem.openFile(path);
                    }
                }), "height 20!");
            } else {
                ret.add(lbl, "spanx");
            }
            ret.add(new JSeparator(), "spanx");
        } else {
            ret = new MigPanel("ins 0,wrap 2", "[grow,fill][]", "[][grow,fill]");
        }
        ret.add(new JLabel(_GUI.T.OpenDownloadFolderAction_layoutDialogContent_object_()), "spanx");
        ret.add(super.layoutDialogContent(), "spanx");
        return ret;
    }

    protected void modifiyNamePanel(JPanel namePanel) {
        if (isPackageSubFolderSelectionVisible()) {
            namePanel.setLayout(new MigLayout("ins 0, wrap 2", "[][grow,fill]", "[][]"));
            namePanel.add(new JLabel(_GUI.T.SetDownloadFolderInDownloadTableAction_modifiyNamePanel_package_()));
            cbPackage = new javax.swing.JCheckBox();
            cbPackage.setSelected(subfolderFlag);
            namePanel.add(cbPackage);
        }
    }

    /**
     * checks if the given file is valid as a downloadfolder, this means it must be an existing folder or at least its parent folder must
     * exist
     *
     * @param file
     * @return
     */
    public static File open(File path, boolean packager, String title) throws DialogClosedException, DialogCanceledException {
        if (path != null && !CrossSystem.isAbsolutePath(path.getPath())) {
            path = new File(CFG_GENERAL.DEFAULT_DOWNLOAD_FOLDER.getValue(), path.getPath());
        }
        boolean lastUsedPathMode = false;
        switch (CFG_GUI.CFG.getDownloadFolderChooserDefaultPath()) {
        case CURRENT_PATH:
            break;
        case GLOBAL_DOWNLOAD_DIRECTORY:
            path = new File(CFG_GENERAL.DEFAULT_DOWNLOAD_FOLDER.getValue());
            break;
        case LAST_USED_PATH:
            lastUsedPathMode = true;
            path = null;
            break;
        }
        final File finalPath = path;
        final DownloadFolderChooserDialog d = new DownloadFolderChooserDialog(path, title, _GUI.T.OpenDownloadFolderAction_actionPerformed_save_(), null);
        d.setPackageSubFolderSelectionVisible(packager);
        if (CrossSystem.isOpenFileSupported()) {
            d.setLeftActions(new AppAction() {
                {
                    setName(_GUI.T.OpenDownloadFolderAction_actionPerformed_button_());
                }

                @Override
                public void actionPerformed(ActionEvent e) {
                    CrossSystem.openFile(d.getSelection()[0] == null ? finalPath : d.getSelection()[0]);
                }
            });
        }
        final List<String> quick = DownloadPathHistoryManager.getInstance().listPaths(path != null ? path.getAbsolutePath() : null);
        d.setQuickSelectionList(quick);
        if (lastUsedPathMode && quick != null && quick.size() > 0) {
            d.setPreSelection(new File(quick.get(0)));
        }
        d.setFileSelectionMode(FileChooserSelectionMode.DIRECTORIES_ONLY);
        final File[] dest = Dialog.getInstance().showDialog(d);
        if (dest == null || dest.length == 0) {
            return null;
        }
        dest[0] = FolderChooser.checkPath(dest[0], null);
        if (dest[0] == null) {
            return null;
        }
        return dest[0];
    }

    private void setPackageSubFolderSelectionVisible(boolean packager) {
        this.packageSubFolderSelectionVisible = packager;
    }

    public boolean isPackageSubFolderSelectionVisible() {
        return packageSubFolderSelectionVisible;
    }
}
