package org.jdownloader.api.content.v2;

import org.appwork.remoteapi.annotations.AllowNonStorableObjects;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storable;
import org.jdownloader.myjdownloader.client.bindings.MenuStructure;

@AllowNonStorableObjects(value = { MenuStructure.class })
public class MyJDMenuItem extends org.jdownloader.myjdownloader.client.bindings.MenuStructure implements Storable {
    public MyJDMenuItem(Type c, String id, String name, String iconKey) {
        setType(c);
        setName(name);
        setIcon(iconKey);
        setId(id);
    }

    @Override
    public String toString() {
        return JSonStorage.serializeToJson(this);
    }

    public MyJDMenuItem() {
        super();
    }
}
