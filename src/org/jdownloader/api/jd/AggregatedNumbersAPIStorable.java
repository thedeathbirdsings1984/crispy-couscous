package org.jdownloader.api.jd;

import org.appwork.storage.Storable;
import org.appwork.storage.StorableValidatorIgnoresMissingSetter;
import org.jdownloader.controlling.AggregatedCrawlerNumbers;
import org.jdownloader.controlling.AggregatedNumbers;

@StorableValidatorIgnoresMissingSetter
public class AggregatedNumbersAPIStorable implements Storable {
    public AggregatedNumbersAPIStorable(/* Storable */) {
    }

    private AggregatedNumbers        aggregated;
    private AggregatedCrawlerNumbers aggregatedCrawler;

    public AggregatedNumbersAPIStorable(AggregatedNumbers aggregated, AggregatedCrawlerNumbers aggregatedCrawler) {
        // SelectionInfo<FilePackage, DownloadLink> sel = new SelectionInfo<FilePackage, DownloadLink>(dc.getAllDownloadLinks());
        this.aggregated = aggregated;
        this.aggregatedCrawler = aggregatedCrawler;
    }

    /*
     * Downloads
     */
    public Integer getPackageCount() {
        return aggregated.getPackageCount();
    }

    public Integer getLinksCount() {
        return aggregated.getLinkCount();
    }

    public Long getTotalBytes() {
        return aggregated.getTotalBytes();
    }

    public Long getDownloadSpeed() {
        return aggregated.getDownloadSpeed();
    }

    public Long getLoadedBytes() {
        return aggregated.getLoadedBytes();
    }

    public Long getETA() {
        final Long ret = aggregated.getEta();
        if (ret == null || ret.longValue() < 0) {
            return 0l;
        } else {
            return ret.longValue();
        }
    }

    public Integer getRunning() {
        return aggregated.getRunning();
    }

    public Integer getConnections() {
        return aggregated.getConnections();
    }

    /*
     * Links
     */
    public Integer getCrawledPackageCount() {
        return aggregatedCrawler.getPackageCount();
    }

    public Integer getCrawledLinksCount() {
        return aggregatedCrawler.getLinkCount();
    }

    public Long getTotalCrawledBytes() {
        return aggregatedCrawler.getTotalBytes();
    }

    public Long getCrawledStatusOnline() {
        return aggregatedCrawler.getStatusOnline();
    }

    public Long getCrawledStatusOffline() {
        return aggregatedCrawler.getStatusOffline();
    }

    public Long getCrawledStatusUnknown() {
        return aggregatedCrawler.getStatusUnknown();
    }
}
