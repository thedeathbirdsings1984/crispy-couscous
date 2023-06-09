package org.jdownloader.api.polling;

import java.util.List;

import org.appwork.remoteapi.APIQuery;
import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.annotations.APIParameterNames;
import org.appwork.remoteapi.annotations.ApiNamespace;

@ApiNamespace("polling")
public interface PollingAPI extends RemoteAPIInterface {
    @APIParameterNames({ "queryParams" })
    List<PollingResultAPIStorable> poll(RemoteAPIRequest request, APIQuery queryParams);
}
