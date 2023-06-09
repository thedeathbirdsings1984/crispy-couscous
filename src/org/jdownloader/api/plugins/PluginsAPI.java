package org.jdownloader.api.plugins;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.annotations.APIParameterNames;
import org.appwork.remoteapi.annotations.AllowNonStorableObjects;
import org.appwork.remoteapi.annotations.ApiNamespace;
import org.appwork.remoteapi.exceptions.BadParameterException;
import org.jdownloader.api.config.AdvancedConfigQueryStorable;
import org.jdownloader.api.config.InvalidValueException;

@ApiNamespace(org.jdownloader.myjdownloader.client.bindings.interfaces.PluginsInterface.NAMESPACE)
public interface PluginsAPI extends RemoteAPIInterface {
    @APIParameterNames({ "URL" })
    List<String> getPluginRegex(String URL);

    HashMap<String, ArrayList<String>> getAllPluginRegex();

    @APIParameterNames({ "query" })
    List<PluginAPIStorable> list(PluginsQueryStorable query);

    @AllowNonStorableObjects
    @APIParameterNames({ "interfaceName", "displayName", "key", "newValue" })
    boolean set(final String interfaceName, final String displayName, String key, final Object newValue) throws BadParameterException, InvalidValueException;

    @APIParameterNames({ "interfaceName", "displayName", "key" })
    boolean reset(final String interfaceName, final String displayName, String key) throws BadParameterException, InvalidValueException;

    @AllowNonStorableObjects(value = { Object.class })
    @APIParameterNames({ "interfaceName", "displayName", "key" })
    public Object get(String interfaceName, String displayName, String key) throws BadParameterException;

    @APIParameterNames({ "query" })
    List<PluginConfigEntryAPIStorable> query(AdvancedConfigQueryStorable query) throws BadParameterException;

    @APIParameterNames({ "URL" })
    List<PluginAPIStorable> getPluginInfos(String URL);
}