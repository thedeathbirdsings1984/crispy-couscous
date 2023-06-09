package org.jdownloader.controlling.contextmenu;

import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledExecutorService;

import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;

import jd.SecondLevelLaunch;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.JsonConfig;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.windowmanager.WindowManager;
import org.appwork.utils.swing.windowmanager.WindowManager.FrameState;
import org.jdownloader.controlling.contextmenu.gui.ExtPopupMenu;
import org.jdownloader.controlling.contextmenu.gui.MenuBuilder;
import org.jdownloader.controlling.contextmenu.gui.MenuManagerDialog;
import org.jdownloader.controlling.contextmenu.gui.MenuManagerDialogInterface;
import org.jdownloader.logging.LogController;

public abstract class ContextMenuManager<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> {
    protected final DelayedRunnable               updateDelayer;
    private static final ScheduledExecutorService SERVICE = DelayedRunnable.getNewScheduledExecutorService();

    public ContextMenuManager() {
        if (Application.isHeadless()) {
            updateDelayer = null;
            return;
        }
        config = JsonConfig.create(Application.getResource("cfg/menus_v2/" + getStorageKey()), ContextMenuConfigInterface.class);
        logger = LogController.getInstance().getLogger(getClass().getName());
        updateDelayer = new DelayedRunnable(SERVICE, 1000l, 2000) {
            @Override
            public String getID() {
                return "MenuManager-" + ContextMenuManager.this.getClass();
            }

            @Override
            public void delayedrun() {
                updateGui();
            }
        };
    }

    public void exportTo(File saveTo) throws UnsupportedEncodingException, IOException {
        File file = null;
        int i = 1;
        while (file == null || file.exists()) {
            file = new File(saveTo, getStorageKey() + "_" + i + getFileExtension());
            i++;
        }
        saveTo(config.getMenu(), file);
    }

    protected abstract String getStorageKey();

    protected static MenuItemData setAccelerator(MenuItemData menuItemData, KeyStroke keyStroke) {
        menuItemData.setShortcut(keyStroke == null ? null : keyStroke.toString());
        return menuItemData;
    }

    protected static ActionData setName(ActionData actionData, String name) {
        if (StringUtils.isEmpty(name)) {
            name = MenuItemData.EMPTY;
        }
        actionData.setName(name);
        return actionData;
    }

    protected static ActionData setTooltip(ActionData actionData, String tooltip) {
        if (StringUtils.isEmpty(tooltip)) {
            tooltip = MenuItemData.EMPTY;
        }
        actionData.setTooltip(tooltip);
        return actionData;
    }

    protected static MenuItemData setOptional(Class<?> class1) {
        return setOptional(new MenuItemData(new ActionData(class1)));
    }

    protected static MenuItemData setOptional(MenuItemData menuItemData) {
        menuItemData.setVisible(false);
        return menuItemData;
    }

    protected static ActionData setIconKey(ActionData putSetup, String KEY) {
        putSetup.setIconKey(KEY);
        return putSetup;
    }

    protected abstract void updateGui();

    public JPopupMenu build(MouseEvent ev) {
        final ExtPopupMenu root = new ExtPopupMenu();
        final MenuContainerRoot md = getMenuData();
        new MenuBuilder(this, root, md).setHideOnClick(!ev.isShiftDown()).run();
        return root;
    }

    private boolean             managerVisible = false;
    protected MenuManagerDialog dialogFrame;

    public void openGui() {
        new Thread("Manager") {
            public void run() {
                if (managerVisible) {
                    new EDTRunner() {
                        @Override
                        protected void runInEDT() {
                            WindowManager.getInstance().setZState(dialogFrame.getDialog(), FrameState.TO_FRONT_FOCUSED);
                        }
                    };
                    return;
                }
                managerVisible = true;
                try {
                    UIOManager.I().show(MenuManagerDialogInterface.class, dialogFrame = new MenuManagerDialog(ContextMenuManager.this));
                } finally {
                    managerVisible = false;
                }
            }
        }.start();
    }

    public LogSource getLogger() {
        return logger;
    }

    private volatile MenuContainerRoot menuData;
    ContextMenuConfigInterface         config;
    LogSource                          logger;

    public List<Object> list() {
        ArrayList<Object> ret = new ArrayList<Object>();
        ret.add(new SeparatorData());
        for (MenuItemData mid : setupDefaultStructure().list()) {
            if (mid instanceof MenuContainerRoot) {
                continue;
            }
            if (MenuContainer.class.isAssignableFrom(mid.getClass().getSuperclass())) {
                if (StringUtils.isEmpty(mid.getName())) {
                    continue;
                }
                if (mid.getClass() == MenuContainer.class) {
                    continue;
                }
                ret.add(mid);
            } else if (mid instanceof MenuLink) {
                ret.add(mid);
            } else if (!(mid instanceof MenuLink) && mid.getActionData() != null && mid.getActionData()._isValidDataForCreatingAnAction()) {
                ret.add(mid.getActionData());
            }
        }
        return ret;
    }

    private final CopyOnWriteArraySet<MenuExtenderHandler> extender = new CopyOnWriteArraySet<MenuExtenderHandler>();
    private Runnable                                       afterInitCallback;

    // public void addExtensionAction(MenuContainerRoot parent, int index, MenuExtenderHandler extender, ExtensionContextMenuItem
    // archiveSubMenu) {
    // if (extender == null) throw new NullPointerException();
    // archiveSubMenu.setOwner(extender.getClass().getSimpleName());
    //
    // if (archiveSubMenu instanceof MenuLink) {
    // addSpecial(archiveSubMenu);
    // }
    // parent.getItems().add(index, archiveSubMenu);
    //
    // }
    public MenuContainerRoot getMenuData() {
        if (!SecondLevelLaunch.EXTENSIONS_LOADED.isReached()) {
            // the menus will get refreshed anyway after the extensioncontroller has been loaded.
            return new MenuContainerRoot();
        }
        return new EDTHelper<MenuContainerRoot>() {
            @Override
            public MenuContainerRoot edtRun() {
                long t = System.currentTimeMillis();
                if (menuData != null) {
                    return menuData;
                }
                try {
                    convertOldFiles();
                    MenuContainerRoot ret = config.getMenu();
                    MenuContainerRoot defaultMenu = setupDefaultStructure();
                    if (ret == null) {
                        // no customizer ever used
                        ret = defaultMenu;
                    } else {
                        ret._setOwner(ContextMenuManager.this);
                        ret.validate();
                        List<MenuItemData> allItemsInMenu = ret.list();
                        List<MenuItemData> allItemsInDefaultMenu = defaultMenu.list();
                        HashMap<String, MenuItemData> itemsIdsInMenu = new HashMap<String, MenuItemData>();
                        HashMap<String, MenuItemData> itemsInDefaultMenu = new HashMap<String, MenuItemData>();
                        for (MenuItemData d : allItemsInDefaultMenu) {
                            itemsInDefaultMenu.put(d._getIdentifier(), d);
                        }
                        for (MenuItemData d : allItemsInMenu) {
                            itemsIdsInMenu.put(d._getIdentifier(), d);
                        }
                        ArrayList<String> unused = config.getUnusedItems();
                        if (unused == null) {
                            unused = new ArrayList<String>();
                        }
                        ArrayList<MenuItemData> newActions = new ArrayList<MenuItemData>();
                        HashSet<String> idsInUnusedList = new HashSet<String>(unused);
                        // find new or updated actions
                        for (Entry<String, MenuItemData> e : itemsInDefaultMenu.entrySet()) {
                            if (!idsInUnusedList.contains(e.getKey())) {
                                // not in unused list
                                if (!itemsIdsInMenu.containsKey(e.getKey())) {
                                    // not in menu itself
                                    // this is a new action
                                    newActions.add(e.getValue());
                                }
                            }
                        }
                        if (newActions.size() > 0) {
                            List<List<MenuItemData>> paths = defaultMenu.listPaths();
                            // HashSet<Class<?>> actionClassesInDefaultTree = new HashSet<Class<?>>();
                            // // HashMap<MenuItemData,> actionClassesInDefaultTree = new HashSet<Class<?>>();
                            //
                            // System.out.println(paths);
                            // for (List<MenuItemData> path : paths) {
                            //
                            // MenuItemData d = path.get(path.size() - 1);
                            // if (d.getActionData() != null) {
                            // if (d.getActionData().getClazzName() != null) {
                            // try {
                            // actionClassesInDefaultTree.add(d.getActionData()._getClazz());
                            // } catch (Exception e1) {
                            // logger.log(e1);
                            // }
                            // }
                            // }
                            // }
                            HashSet<String> itemsInSubmenuItems = new HashSet<String>();
                            for (MenuItemData ad : newActions) {
                                if (ad.getItems() != null) {
                                    for (MenuItemData mid : ad.list()) {
                                        if (mid == ad) {
                                            continue;
                                        }
                                        // newActions.remove(mid);
                                        itemsInSubmenuItems.add(mid._getIdentifier());
                                    }
                                }
                            }
                            for (MenuItemData ad : newActions) {
                                if (itemsInSubmenuItems.contains(ad._getIdentifier())) {
                                    continue;
                                }
                                for (List<MenuItemData> path : paths) {
                                    if (StringUtils.equals(path.get(path.size() - 1)._getIdentifier(), ad._getIdentifier())) {
                                        try {
                                            ret.add(path);
                                        } catch (Throwable e) {
                                            logger.log(e);
                                        }
                                    }
                                }
                            }
                            // neworUpdate.add(new SeparatorData());
                            // neworUpdate.add(new MenuItemData(new ActionData(0,MenuManagerAction.class)));
                        }
                    }
                    ret._setOwner(ContextMenuManager.this);
                    ret.validate();
                    menuData = ret;
                    onSetupMenuData(menuData);
                    // System.out.println(System.currentTimeMillis() - t);
                    return ret;
                } catch (Exception e) {
                    logger.log(e);
                    try {
                        menuData = setupDefaultStructure();
                        menuData.validate();
                        onSetupMenuData(menuData);
                        return menuData;
                    } catch (Exception e1) {
                        logger.log(e1);
                        menuData = new MenuContainerRoot();
                        menuData._setOwner(ContextMenuManager.this);
                        menuData.validate();
                        onSetupMenuData(menuData);
                        return menuData;
                    }
                }
            }
        }.getReturnValue();
    }

    protected void onSetupMenuData(MenuContainerRoot menuData) {
    }

    private void convertOldFiles() {
    }

    abstract public MenuContainerRoot createDefaultStructure();

    // public void extend(JComponent root, ExtensionContextMenuItem<?> inst, SelectionInfo<?, ?> selection, MenuContainerRoot menuData) {
    // synchronized (extender) {
    // for (MenuExtenderHandler exHandler : extender) {
    // exHandler.extend(root, inst, selection, menuData);
    //
    // }
    // }
    // }
    public MenuContainerRoot setupDefaultStructure() {
        MenuContainerRoot ret = createDefaultStructure();
        ret._setOwner(this);
        for (MenuExtenderHandler exHandler : extender) {
            try {
                MenuItemData r = exHandler.updateMenuModel(this, ret);
                if (r != null) {
                    ret.addBranch(ret, r);
                }
            } catch (final Throwable e) {
                getLogger().log(e);
            }
        }
        return ret;
    }

    public void setMenuData(MenuContainerRoot root) {
        final String rootString = JSonStorage.serializeToJson(root);
        final String orgDefString = JSonStorage.serializeToJson(setupDefaultStructure());
        final MenuContainerRoot def = JSonStorage.restoreFromString(orgDefString, new TypeRef<MenuContainerRoot>() {
        });
        def._setRoot(root);
        def._setOwner(root._getOwner());
        def.validate();
        final String defaultString = JSonStorage.serializeToJson(def);
        if (rootString.equals(defaultString)) {
            root = null;
        }
        if (root == null) {
            config.setMenu(null);
            config.setUnusedItems(null);
            menuData = setupDefaultStructure();
            menuData.validate();
        } else {
            menuData = root;
            config.setMenu(root);
            config.setUnusedItems(getUnused(root));
        }
        updateGui();
    }

    private ArrayList<String> getUnused(MenuContainerRoot root) {
        ArrayList<String> list = new ArrayList<String>();
        List<MenuItemData> allItemsInMenu = root.list();
        HashSet<String> actionClassesInMenu = new HashSet<String>();
        for (MenuItemData d : allItemsInMenu) {
            actionClassesInMenu.add(d._getIdentifier());
        }
        // HashSet<String> actionClassesInDefaultMenu = new HashSet<String>();
        for (MenuItemData e : setupDefaultStructure().list()) {
            if (actionClassesInMenu.add(e._getIdentifier())) {
                list.add(e._getIdentifier());
            }
        }
        return list;
    }

    public void saveTo(MenuContainerRoot root, File saveTo) throws UnsupportedEncodingException, IOException {
        if (root == null) {
            Dialog.getInstance().showErrorDialog("Cannot export '" + getName() + "', because there is no custom menu");
        } else {
            IO.secureWrite(saveTo, JSonStorage.serializeToJson(new MenuStructure(root, getUnused(root))).getBytes("UTF-8"));
        }
    }

    public void importFrom(File f) throws IOException {
        MenuStructure data = readFrom(f);
        data.getRoot().validateFull();
        setMenuData(data.getRoot());
        Dialog.getInstance().showMessageDialog("Imported '" + getName() + "");
    }

    public MenuStructure readFrom(File file) throws IOException {
        return JSonStorage.restoreFromString(IO.readFileToString(file), new TypeRef<MenuStructure>() {
        });
    }

    public void registerExtender(MenuExtenderHandler handler) {
        if (!Application.isHeadless()) {
            if (extender.add(handler)) {
                new EDTRunner() {
                    @Override
                    protected void runInEDT() {
                        menuData = null;
                    }
                };
                delayUpdate();
            }
        }
    }

    public void refresh() {
        delayUpdate();
    }

    public void unregisterExtender(MenuExtenderHandler handler) {
        if (!Application.isHeadless()) {
            if (extender.remove(handler)) {
                new EDTRunner() {
                    @Override
                    protected void runInEDT() {
                        menuData = null;
                    }
                };
                delayUpdate();
            }
        }
    }

    private void delayUpdate() {
        if (Application.isHeadless()) {
            new Exception("Headless!").printStackTrace();
            return;
        }
        if (!SecondLevelLaunch.EXTENSIONS_LOADED.isReached()) {
            if (afterInitCallback != null) {
                return;
            }
            SecondLevelLaunch.EXTENSIONS_LOADED.executeWhenReached(afterInitCallback = new Runnable() {
                @Override
                public void run() {
                    updateDelayer.resetAndStart();
                }
            });
        } else {
            afterInitCallback = null;
            updateDelayer.resetAndStart();
        }
    }

    public ArrayList<MenuExtenderHandler> listExtender() {
        return new ArrayList<MenuExtenderHandler>(extender);
    }

    public abstract String getFileExtension();

    public abstract String getName();

    public boolean isAcceleratorsEnabled() {
        return false;
    }
}
