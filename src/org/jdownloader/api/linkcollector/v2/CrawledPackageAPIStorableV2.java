package org.jdownloader.api.linkcollector.v2;

import jd.controlling.linkcrawler.CrawledPackage;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storable;
import org.jdownloader.myjdownloader.client.bindings.linkgrabber.CrawledPackageStorable;

public class CrawledPackageAPIStorableV2 extends CrawledPackageStorable implements Storable {
    public static void main(String[] args) {
        System.out.println(CrawledPackageAPIStorableV2.class.getSimpleName() + "= ");
        System.out.println(JSonStorage.toString(new CrawledPackageAPIStorableV2()));
    }

    public CrawledPackageAPIStorableV2(/* STorable */) {
    }

    public CrawledPackageAPIStorableV2(CrawledPackage pkg) {
        setName(pkg.getName());
        setUuid(pkg.getUniqueID().getID());
    }
}
