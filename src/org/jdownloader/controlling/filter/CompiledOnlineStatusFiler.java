package org.jdownloader.controlling.filter;

import org.appwork.storage.Storable;
import org.jdownloader.myjdownloader.client.json.AvailableLinkState;

import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.OnlineStatusFilter;

public class CompiledOnlineStatusFiler extends OnlineStatusFilter implements Storable {
    private CompiledOnlineStatusFiler() {
    }

    public CompiledOnlineStatusFiler(OnlineStatusFilter onlineStatusFilter) {
        super(onlineStatusFilter.getMatchType(), onlineStatusFilter.isEnabled(), onlineStatusFilter.getOnlineStatus());
    }

    public boolean matches(AvailableLinkState linkState) {
        switch (getMatchType()) {
        case IS:
            switch (getOnlineStatus()) {
            case OFFLINE:
                return linkState == AvailableLinkState.OFFLINE;
            case ONLINE:
                return linkState == AvailableLinkState.ONLINE;
            case UNCHECKABLE:
                return linkState == AvailableLinkState.TEMP_UNKNOWN || linkState == AvailableLinkState.UNKNOWN;
            }
            break;
        case ISNOT:
            switch (getOnlineStatus()) {
            case OFFLINE:
                return linkState != AvailableLinkState.OFFLINE;
            case ONLINE:
                return linkState != AvailableLinkState.ONLINE;
            case UNCHECKABLE:
                return linkState != AvailableLinkState.TEMP_UNKNOWN && linkState != AvailableLinkState.UNKNOWN;
            }
            break;
        }
        return false;
    }
}
