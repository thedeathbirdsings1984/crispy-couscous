package org.jdownloader.api.polling;

import java.util.ArrayList;
import java.util.List;

import org.appwork.storage.Storable;
import org.appwork.storage.StorableValidatorIgnoresMissingSetter;
import org.jdownloader.controlling.DownloadLinkAggregator;

import jd.plugins.FilePackage;

@StorableValidatorIgnoresMissingSetter
public class PollingAPIFilePackageStorable implements Storable {
    private FilePackage                          fp;
    private Long                                 speed;
    private List<PollingAPIDownloadLinkStorable> downloadLinks = new ArrayList<PollingAPIDownloadLinkStorable>();
    private DownloadLinkAggregator               numbers;

    public PollingAPIFilePackageStorable(/* Storable */) {
        this.fp = null;
    }

    public PollingAPIFilePackageStorable(FilePackage link) {
        this.fp = link;
        numbers = new DownloadLinkAggregator(fp);
    }

    public Long getUUID() {
        return fp.getUniqueID().getID();
    }

    public Long getDone() {
        return (long) numbers.getFinishedCount();
    }

    public Boolean getFinished() {
        return numbers.isFinished();
    }

    public Long getSize() {
        return numbers.getTotalBytes();
    }

    public void _setSpeed(Long speed) {
        this.speed = speed;
    }

    public Long getSpeed() {
        return speed;
    }

    public Long getETA() {
        return numbers.getEta();
    }

    public List<PollingAPIDownloadLinkStorable> getLinks() {
        return downloadLinks;
    }

    public void _setLinks(List<PollingAPIDownloadLinkStorable> downloadLinks) {
        this.downloadLinks = downloadLinks;
    }
}
