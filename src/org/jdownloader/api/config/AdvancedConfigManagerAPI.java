package org.jdownloader.api.config;

import java.util.ArrayList;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.annotations.APIParameterNames;
import org.appwork.remoteapi.annotations.AllowNonStorableObjects;
import org.appwork.remoteapi.annotations.ApiDoc;
import org.appwork.remoteapi.annotations.ApiNamespace;
import org.appwork.remoteapi.exceptions.BadParameterException;
import org.jdownloader.settings.advanced.AdvancedConfigAPIEntry;

@ApiNamespace(org.jdownloader.myjdownloader.client.bindings.interfaces.AdvancedConfigInterface.NAMESPACE)
public interface AdvancedConfigManagerAPI extends RemoteAPIInterface {
    @ApiDoc("list all possible enum values")
    @AllowNonStorableObjects
    @APIParameterNames({ "type" })
    public ArrayList<EnumOption> listEnum(String type) throws BadParameterException;

    @ApiDoc("list all available config entries")
    @AllowNonStorableObjects
    public ArrayList<AdvancedConfigAPIEntry> list();

    @ApiDoc("list entries based on the pattern regex")
    @AllowNonStorableObjects
    @APIParameterNames({ "pattern", "returnDescription", "returnValues", "returnDefaultValues", "returnEnumInfo" })
    public ArrayList<AdvancedConfigAPIEntry> list(String pattern, boolean returnDescription, boolean returnValues, boolean returnDefaultValues, boolean returnEnumInfo);

    @Deprecated
    @ApiDoc("DEPRECATED! list entries based on the pattern regex")
    @AllowNonStorableObjects
    @APIParameterNames({ "pattern", "returnDescription", "returnValues", "returnDefaultValues" })
    public ArrayList<AdvancedConfigAPIEntry> list(String pattern, boolean returnDescription, boolean returnValues, boolean returnDefaultValues);

    @AllowNonStorableObjects(value = { Object.class })
    @ApiDoc("get value from interface by key")
    @APIParameterNames({ "interfaceName", "storage", "key" })
    public Object get(String interfaceName, String storage, String key);

    @AllowNonStorableObjects
    @ApiDoc("set value to interface by key")
    @APIParameterNames({ "interfaceName", "storage", "key", "value" })
    public boolean set(RemoteAPIRequest request, String interfaceName, String storage, String key, Object value) throws InvalidValueException;

    @ApiDoc("reset interface by key to its default value")
    @APIParameterNames({ "interfaceName", "storage", "key" })
    public boolean reset(RemoteAPIRequest request, String interfaceName, String storage, String key);

    @AllowNonStorableObjects(value = { Object.class })
    @ApiDoc("get default value from interface by key")
    @APIParameterNames({ "interfaceName", "storage", "key" })
    public Object getDefault(String interfaceName, String storage, String key);

    @AllowNonStorableObjects(value = { Object.class })
    @APIParameterNames({ "query" })
    public ArrayList<AdvancedConfigAPIEntry> query(AdvancedConfigQueryStorable query);
}
