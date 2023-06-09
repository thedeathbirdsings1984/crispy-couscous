package org.jdownloader.controlling.contextmenu;

import java.util.TreeMap;

import org.appwork.remoteapi.annotations.AllowNonStorableObjects;
import org.appwork.storage.Storable;
import org.appwork.utils.StringUtils;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.extensions.ExtensionNotLoadedException;
import org.jdownloader.myjdownloader.client.json.AbstractJsonData;

public class ActionData extends AbstractJsonData implements Storable {
    private Class<?>            clazz;
    private static final String PACKAGE_NAME = AbstractExtension.class.getPackage().getName() + ".";
    private String              data;

    public void setData(String data) {
        this.data = data;
    }

    public String getData() {
        return data;
    }

    public boolean _isValidDataForCreatingAnAction() {
        try {
            return _getClazz() != null;
        } catch (Throwable e) {
            return false;
        }
    }

    public Class<?> _getClazz() throws ClassNotFoundException, ExtensionNotLoadedException {
        if (clazz == null) {
            if (getClazzName() == null) {
                return null;
            }
            if (_isExtensionAction()) {
                clazz = ExtensionController.getInstance().loadClass(getClazzName());
            } else {
                clazz = Class.forName(getClazzName());
            }
        } else if (_isExtensionAction()) {
            clazz = ExtensionController.getInstance().loadClass(getClazzName());
        }
        return clazz;
    }

    private boolean _isExtensionAction() {
        String cn = getClazzName();
        if (cn == null) {
            return false;
        }
        int i = cn.lastIndexOf(".");
        String pkg = i >= 0 ? cn.substring(0, i) : "";
        boolean ret = pkg.startsWith(PACKAGE_NAME);
        return ret;
    }

    private String                  clazzName;
    private String                  name;
    private String                  iconKey;
    private TreeMap<String, Object> setup;
    private String                  tooltip;

    public ActionData(/* Storable */) {
    }

    public ActionData(Class<?> class1) {
        this(class1, null);
    }

    public ActionData(Class<?> class1, String data) {
        this.data = data;
        this.clazz = class1;
        this.clazzName = class1.getName();
    }

    public String getClazzName() {
        return clazzName;
    }

    public void setClazzName(String clazzName) {
        this.clazzName = clazzName;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getIconKey() {
        return iconKey;
    }

    public void setIconKey(String iconKey) {
        this.iconKey = iconKey;
    }

    public ActionData putSetup(String key, Object value) {
        if (setup == null) {
            setup = new TreeMap<String, Object>();
        }
        setup.put(StringUtils.toUpperCaseOrNull(key), value);
        return this;
    }

    @AllowNonStorableObjects
    public TreeMap<String, Object> getSetup() {
        return setup;
    }

    public void setSetup(TreeMap<String, Object> setup) {
        this.setup = setup;
    }

    public Object fetchSetup(String name2) {
        if (setup == null) {
            return null;
        } else {
            return setup.get(StringUtils.toUpperCaseOrNull(name2));
        }
    }

    public void setTooltip(String tooltip) {
        this.tooltip = tooltip;
    }

    public String getTooltip() {
        return tooltip;
    }
}
