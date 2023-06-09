package org.jdownloader.api.jdanywhere.api.interfaces;

import java.util.List;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.remoteapi.annotations.AllowNonStorableObjects;
import org.appwork.remoteapi.annotations.ApiNamespace;
import org.appwork.remoteapi.annotations.ApiSessionRequired;
import org.appwork.remoteapi.exceptions.InternalApiException;
import org.jdownloader.api.jdanywhere.api.storable.RunningObjectStorable;

@ApiNamespace("jdanywhere/dashboard")
@ApiSessionRequired
public interface IDashboardApi extends RemoteAPIInterface {
    public abstract List<Integer> speedList();

    public abstract boolean start();

    public abstract boolean stop();

    public abstract boolean pause();

    // paused = 1
    // stopped = 2
    // running = 0
    public abstract String getState();

    // returns the current downloadspeed
    // used in iPhone-App
    public abstract int speed();

    // returns the current downloadlimit
    // used in iPhone-App
    public abstract int limit();

    // returns the current traffic
    // used in iPhone-App
    public abstract long traffic();

    public abstract boolean setLimitspeed(int speed);

    public abstract boolean activateLimitspeed(boolean activate);

    // returns the SpeedMeter from UI without the DownloadSpeed / AverageSpeed Text as an PNG
    // used in iPhone-App
    public abstract void speedMeter(RemoteAPIRequest request, RemoteAPIResponse response) throws InternalApiException;

    public abstract List<RunningObjectStorable> runningLinks();

    @AllowNonStorableObjects(value = { Object.class })
    public Object getCompleteState();

    public boolean setMaxDL(int value);

    public boolean setMaxConDL(int value);

    public boolean setMaxConHost(int value);

    public boolean activateMaxConHost(boolean value);

    public boolean activateReconnect(boolean value);

    public boolean activatePremium(boolean value);

    public String apiVersion();
}