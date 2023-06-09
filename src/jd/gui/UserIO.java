//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.gui;

import java.io.File;

import javax.swing.Icon;
import javax.swing.JFileChooser;
import javax.swing.ListCellRenderer;
import javax.swing.filechooser.FileFilter;

import jd.nutils.JDFlags;

import org.appwork.storage.config.JsonConfig;
import org.appwork.uio.ComboBoxDialogInterface;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.InputDialogInterface;
import org.appwork.uio.MultiSelectionDialog;
import org.appwork.uio.MultiSelectionDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.uio.UserIODefinition;
import org.appwork.utils.BinaryLogic;
import org.appwork.utils.swing.dialog.ComboBoxDialog;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.appwork.utils.swing.dialog.ExtFileChooserDialog;
import org.appwork.utils.swing.dialog.FileChooserSelectionMode;
import org.appwork.utils.swing.dialog.FileChooserType;
import org.appwork.utils.swing.dialog.InputDialog;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class UserIO {
    public static final int FILES_ONLY                     = JFileChooser.FILES_ONLY;
    public static final int DIRECTORIES_ONLY               = JFileChooser.DIRECTORIES_ONLY;
    public static final int FILES_AND_DIRECTORIES          = JFileChooser.FILES_AND_DIRECTORIES;
    public static final int OPEN_DIALOG                    = JFileChooser.OPEN_DIALOG;
    public static final int SAVE_DIALOG                    = JFileChooser.SAVE_DIALOG;
    /**
     * do not display a countdown
     */
    public static final int NO_COUNTDOWN                   = 1 << 2;
    /**
     * do not display ok option
     */
    public static final int NO_OK_OPTION                   = 1 << 3;
    /**
     * do not display cancel option
     */
    public static final int NO_CANCEL_OPTION               = 1 << 4;
    /**
     * displays a do not show this question again checkbox
     */
    public static final int DONT_SHOW_AGAIN                = 1 << 5;
    /**
     * IF available a large evrsion of the dialog is used
     */
    public static final int STYLE_LARGE                    = 1 << 6;
    /**
     * Render html
     */
    public static final int STYLE_HTML                     = 1 << 7;
    /**
     * Does not display an icon
     */
    public static final int NO_ICON                        = 1 << 8;
    /**
     * Cancel option ignores Don't show again checkbox
     */
    public static final int DONT_SHOW_AGAIN_IGNORES_CANCEL = 1 << 9;
    /**
     * If user selects OK Option, the don't show again option is ignored
     */
    public static final int DONT_SHOW_AGAIN_IGNORES_OK     = 1 << 10;
    /**
     * the textfield will be renderer as a passwordfield
     */
    public static final int STYLE_PASSWORD                 = 1 << 11;
    /**
     * pressed ok
     */
    public static final int RETURN_OK                      = 1 << 1;
    /**
     * pressed cancel
     */
    public static final int RETURN_CANCEL                  = 1 << 2;
    /**
     * don't show again flag has been set. the dialog may has been visible. if RETURN_SKIPPED_BY_DONT_SHOW is not set. the user set this
     * flag latly
     */
    public static final int RETURN_DONT_SHOW_AGAIN         = 1 << 3;
    /**
     * don't show again flag has been set the dialog has not been visible
     */
    public static final int RETURN_SKIPPED_BY_DONT_SHOW    = 1 << 4;
    /**
     * Timeout has run out. Returns current settings or default values
     */
    public static final int RETURN_COUNTDOWN_TIMEOUT       = 1 << 5;
    public static final int ICON_INFO                      = 0;
    public static final int ICON_WARNING                   = 1;
    public static final int ICON_ERROR                     = 2;
    public static final int ICON_QUESTION                  = 3;
    protected static UserIO INSTANCE                       = new UserIO();

    public UserIO() {
        setCountdownTime(UserIO.getUserCountdownTime());
    }

    /**
     * @param countdownTime
     *            sets the countdown time or resets it to the user-selected value, if <code>countdownTime < 0</code>
     */
    @Deprecated
    public static void setCountdownTime(int countdownTime) {
        if (countdownTime < 0) {
            Dialog.getInstance().setDefaultTimeout(UserIO.getUserCountdownTime() * 1000);
        } else {
            Dialog.getInstance().setDefaultTimeout(countdownTime * 1000);
        }
    }

    @Deprecated
    private static int getUserCountdownTime() {
        return Math.max(2, JsonConfig.create(GraphicalUserInterfaceSettings.class).getDialogDefaultTimeoutInMS() / 1000);
    }

    public static UserIO getInstance() {
        return UserIO.INSTANCE;
    }

    public static void setInstance(UserIO userIO) {
        if (userIO == null) {
            throw new RuntimeException("userIO must not be null");
        }
        UserIO.INSTANCE = userIO;
    }

    /**
     * Checks wether this answerfalg contains the ok option
     *
     * @param answer
     * @return
     */
    public static boolean isOK(final int answer) {
        return JDFlags.hasSomeFlags(answer, UserIO.RETURN_OK);
    }

    /**
     * COnverts the flag mask of AW Dialogs to UserIO
     *
     * @param ret
     * @return
     */
    private int convertAWAnswer(final int ret) {
        int response = 0;
        if (BinaryLogic.containsAll(ret, Dialog.RETURN_CANCEL)) {
            response |= UserIO.RETURN_CANCEL;
        }
        if (BinaryLogic.containsAll(ret, Dialog.RETURN_OK)) {
            response |= UserIO.RETURN_OK;
        }
        if (BinaryLogic.containsAll(ret, Dialog.RETURN_CLOSED)) {
            response |= UserIO.RETURN_CANCEL;
        }
        if (BinaryLogic.containsAll(ret, Dialog.RETURN_DONT_SHOW_AGAIN)) {
            response |= UserIO.RETURN_DONT_SHOW_AGAIN;
        }
        if (BinaryLogic.containsAll(ret, Dialog.RETURN_SKIPPED_BY_DONT_SHOW)) {
            response |= UserIO.RETURN_SKIPPED_BY_DONT_SHOW;
        }
        if (BinaryLogic.containsAll(ret, Dialog.RETURN_TIMEOUT)) {
            response |= UserIO.RETURN_COUNTDOWN_TIMEOUT;
        }
        return response;
    }

    /**
     * The flags in org.appwork.utils.swing.dialog.Dialog are different, so we need a converter
     *
     * @param flag
     * @return
     */
    private int convertFlagToAWDialog(final int flag) {
        int ret = 0;
        if (BinaryLogic.containsNone(flag, UserIO.NO_COUNTDOWN)) {
            ret |= UIOManager.LOGIC_COUNTDOWN;
        }
        if (BinaryLogic.containsAll(flag, UserIO.NO_OK_OPTION)) {
            ret |= UIOManager.BUTTONS_HIDE_OK;
        }
        if (BinaryLogic.containsAll(flag, UserIO.NO_CANCEL_OPTION)) {
            ret |= UIOManager.BUTTONS_HIDE_CANCEL;
        }
        if (BinaryLogic.containsAll(flag, UserIO.DONT_SHOW_AGAIN)) {
            ret |= Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN;
        }
        if (BinaryLogic.containsAll(flag, UserIO.STYLE_LARGE)) {
            ret |= Dialog.STYLE_LARGE;
        }
        if (BinaryLogic.containsAll(flag, UserIO.STYLE_HTML)) {
            ret |= Dialog.STYLE_HTML;
        }
        if (BinaryLogic.containsAll(flag, UserIO.NO_ICON)) {
            ret |= Dialog.STYLE_HIDE_ICON;
        }
        if (BinaryLogic.containsAll(flag, UserIO.DONT_SHOW_AGAIN_IGNORES_CANCEL)) {
            ret |= UIOManager.LOGIC_DONT_SHOW_AGAIN_IGNORES_CANCEL;
        }
        if (BinaryLogic.containsAll(flag, UserIO.DONT_SHOW_AGAIN_IGNORES_OK)) {
            ret |= UIOManager.LOGIC_DONT_SHOW_AGAIN_IGNORES_OK;
        }
        if (BinaryLogic.containsAll(flag, UserIO.STYLE_PASSWORD)) {
            ret |= Dialog.STYLE_PASSWORD;
        }
        return ret;
    }

    public static Icon getDefaultIcon(final String text) {
        if (text.contains("?")) {
            return getIcon(UserIO.ICON_QUESTION);
        } else if (text.matches(_GUI.T.userio_errorregex())) {
            return getIcon(UserIO.ICON_ERROR);
        } else if (text.contains("!")) {
            return getIcon(UserIO.ICON_WARNING);
        } else {
            return getIcon(UserIO.ICON_INFO);
        }
    }

    public static Icon getIcon(final int iconInfo) {
        switch (iconInfo) {
        case UserIO.ICON_ERROR:
            return new AbstractIcon(IconKey.ICON_STOP, 32);
        case UserIO.ICON_WARNING:
            return new AbstractIcon(IconKey.ICON_WARNING, 32);
        case UserIO.ICON_QUESTION:
            return new AbstractIcon(IconKey.ICON_HELP, 32);
        default:
            return new AbstractIcon(IconKey.ICON_INFO, 32);
        }
    }

    /**
     * Shows a combobox dialog. returns the options id if the user confirmed, or -1 if the user canceled
     *
     * @param flag
     * @param title
     * @param question
     * @param options
     * @param defaultSelection
     * @param icon
     * @param okText
     * @param cancelText
     * @param renderer
     *            TODO
     * @return
     */
    public int requestComboDialog(int flag, final String title, final String question, final Object[] options, final int defaultSelection, final Icon icon, final String okText, final String cancelText, final ListCellRenderer renderer) {
        flag = this.convertFlagToAWDialog(flag);
        ComboBoxDialog d = new ComboBoxDialog(flag, title, question, options, defaultSelection, icon, okText, cancelText, renderer) {
            @Override
            protected boolean isResizable() {
                return true;
            }

            @Override
            public boolean isRemoteAPIEnabled() {
                return true;
            }
        };
        return UIOManager.I().show(ComboBoxDialogInterface.class, d).getSelectedIndex();
    }

    public int requestConfirmDialog(final int flag, final String question) {
        return this.requestConfirmDialog(flag, _GUI.T.jd_gui_userio_defaulttitle_confirm(), question, getDefaultIcon(question), null, null);
    }

    public int requestConfirmDialog(final int flag, final String title, final String question) {
        return this.requestConfirmDialog(flag, title, question, getDefaultIcon(title + question), null, null);
    }

    public int requestConfirmDialog(final int flag, final String title, final String message, final Icon icon, final String okOption, final String cancelOption) {
        return requestUIOManagerDialog(ConfirmDialogInterface.class, new ConfirmDialog(this.convertFlagToAWDialog(flag), title, message, icon, okOption, cancelOption));
    }

    <T extends UserIODefinition> int requestUIOManagerDialog(Class<T> class1, T impl) {
        int response = 0;
        try {
            T d = UIOManager.I().show(class1, impl);
            switch (d.getCloseReason()) {
            case CANCEL:
            case CLOSE:
            case INTERRUPT:
                response |= UserIO.RETURN_CANCEL;
                break;
            case OK:
                response |= UserIO.RETURN_OK;
                break;
            case TIMEOUT:
                response |= UserIO.RETURN_COUNTDOWN_TIMEOUT;
                break;
            }
            if (d.isDontShowAgainSelected()) {
                response |= UserIO.RETURN_DONT_SHOW_AGAIN;
            }
            d.throwCloseExceptions();
        } catch (DialogClosedException e) {
            response |= UserIO.RETURN_CANCEL;
            if (e.isCausedByDontShowAgain()) {
                response |= UserIO.RETURN_SKIPPED_BY_DONT_SHOW;
            }
            if (e.isCausedByTimeout()) {
                response |= UserIO.RETURN_COUNTDOWN_TIMEOUT;
            }
        } catch (DialogCanceledException e) {
            response |= UserIO.RETURN_CANCEL;
            if (e.isCausedByDontShowAgain()) {
                response |= UserIO.RETURN_SKIPPED_BY_DONT_SHOW;
            }
            if (e.isCausedByTimeout()) {
                response |= UserIO.RETURN_COUNTDOWN_TIMEOUT;
            }
        }
        return response;
    }

    @Deprecated
    public File[] requestFileChooser(final String id, final String title, final Integer fileSelectionMode, final FileFilter fileFilter, final Boolean multiSelection) {
        if (fileFilter == null) {
            return this.requestFileChooser(id, title, fileSelectionMode, multiSelection, null, null, new FileFilter[0]);
        } else {
            return this.requestFileChooser(id, title, fileSelectionMode, multiSelection, null, null, new FileFilter[] { fileFilter });
        }
    }

    public File[] requestFileChooser(final String id, final String title, final Integer fileSelectionMode, final Boolean multiSelection, final FileFilter... fileFilter) {
        return this.requestFileChooser(id, title, fileSelectionMode, multiSelection, null, null, fileFilter);
    }

    /**
     * Requests a FileChooserDialog.
     *
     * @param id
     *            ID of the dialog (used to save and restore the old directory)
     * @param title
     *            dialog-title or null for default
     * @param fileSelectionMode
     *            mode for selecting files (like {@link UserIO#FILES_ONLY}) or null for default
     * @param fileFilter
     *            filters the choosable files or null for default
     * @param multiSelection
     *            multible files choosable? or null for default
     * @param startDirectory
     *            the start directory
     * @param dialogType
     *            mode for the dialog type (like {@link UserIO#OPEN_DIALOG}) or null for default
     * @return an array of files or null if the user cancel the dialog
     */
    @Deprecated
    public File[] requestFileChooser(final String id, final String title, final Integer fileSelectionMode, final FileFilter fileFilter, final Boolean multiSelection, final File startDirectory, final Integer dialogType) {
        if (fileFilter == null) {
            return requestFileChooser(id, title, fileSelectionMode, multiSelection, startDirectory, dialogType, new FileFilter[0]);
        } else {
            return requestFileChooser(id, title, fileSelectionMode, multiSelection, startDirectory, dialogType, new FileFilter[] { fileFilter });
        }
    }

    public File[] requestFileChooser(final String id, final String title, final Integer fileSelectionMode, final Boolean multiSelection, final File startDirectory, final Integer dialogType, final FileFilter... fileFilter) {
        FileChooserSelectionMode fsm = FileChooserSelectionMode.FILES_AND_DIRECTORIES;
        for (final FileChooserSelectionMode f : FileChooserSelectionMode.values()) {
            if (f.getId() == fileSelectionMode) {
                fsm = f;
                break;
            }
        }
        FileChooserType fct = FileChooserType.OPEN_DIALOG;
        if (dialogType != null) {
            for (final FileChooserType f : FileChooserType.values()) {
                if (f.getId() == dialogType) {
                    fct = f;
                    break;
                }
            }
        }
        final ExtFileChooserDialog d = new ExtFileChooserDialog(0, title, null, null);
        d.setStorageID(id);
        d.setFileSelectionMode(fsm);
        d.setFileFilter(fileFilter);
        d.setType(fct);
        d.setMultiSelection(multiSelection != null && multiSelection);
        d.setPreSelection(startDirectory);
        try {
            Dialog.I().showDialog(d);
            return d.getSelection();
        } catch (DialogNoAnswerException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     *
     * @param flag
     *            flag
     * @param question
     *            question
     * @param defaultvalue
     *            defaultvalue
     * @return
     */
    public String requestInputDialog(final int flag, final String question, final String defaultvalue) {
        return this.requestInputDialog(flag, _GUI.T.jd_gui_userio_defaulttitle_input(), question, defaultvalue, this.getDefaultIcon(question), null, null);
    }

    public String requestInputDialog(final int flag, final String title, final String message, final String defaultMessage, final Icon icon, final String okOption, final String cancelOption) {
        try {
            InputDialogInterface d = UIOManager.I().show(InputDialogInterface.class, new InputDialog(this.convertFlagToAWDialog(flag), title, message, defaultMessage, icon, okOption, cancelOption));
            switch (d.getCloseReason()) {
            case OK:
                return d.getText();
            default:
            }
            d.throwCloseExceptions();
        } catch (DialogClosedException e) {
        } catch (DialogCanceledException e) {
        }
        return null;
    }

    public String requestInputDialog(final String message) {
        return this.requestInputDialog(0, message, null);
    }

    public void requestMessageDialog(final int flag, final String message) {
        this.requestMessageDialog(flag, _GUI.T.gui_dialogs_message_title(), message);
    }

    public void requestMessageDialog(final int flag, final String title, final String message) {
        this.requestConfirmDialog(UserIO.NO_CANCEL_OPTION | flag, title, message, this.getIcon(UserIO.ICON_INFO), null, null);
    }

    public void requestMessageDialog(final String message) {
        this.requestMessageDialog(0, _GUI.T.gui_dialogs_message_title(), message);
    }

    public void requestMessageDialog(final String title, final String message) {
        this.requestMessageDialog(0, title, message);
    }

    /**
     * Shows a multi-selection dialog.
     *
     * @return indices of selected options or null if user canceled
     */
    public int[] requestMultiSelectionDialog(final int flag, final String title, final String question, final Object[] options, final Icon icon, final String okText, final String cancelText, final ListCellRenderer renderer) {
        try {
            MultiSelectionDialogInterface d = UIOManager.I().show(MultiSelectionDialogInterface.class, new MultiSelectionDialog(flag, title, question, options, icon, okText, cancelText, renderer) {
                @Override
                public boolean isRemoteAPIEnabled() {
                    return true;
                }
            });
            d.throwCloseExceptions();
            return d.getSelectedIndices();
        } catch (DialogClosedException e) {
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            e.printStackTrace();
        }
        return null;
    }
}