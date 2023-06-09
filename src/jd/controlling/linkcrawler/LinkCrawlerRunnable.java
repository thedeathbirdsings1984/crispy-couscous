package jd.controlling.linkcrawler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import jd.controlling.linkcrawler.LinkCrawler.LinkCrawlerGeneration;
import jd.controlling.linkcrawler.LinkCrawler.LinkCrawlerTask;

public abstract class LinkCrawlerRunnable implements Runnable {
    private final LinkCrawlerGeneration generation;

    public LinkCrawlerGeneration getGeneration() {
        return generation;
    }

    private final LinkCrawler                                         crawler;
    private final LinkCrawlerTask                                     task;
    static final HashMap<Object, java.util.List<LinkCrawlerRunnable>> SEQ_RUNNABLES = new HashMap<Object, java.util.List<LinkCrawlerRunnable>>();
    static final HashMap<Object, AtomicInteger>                       SEQ_COUNTER   = new HashMap<Object, AtomicInteger>();

    protected LinkCrawlerRunnable(LinkCrawler crawler, final LinkCrawlerGeneration generation, final LinkCrawlerTask task) {
        if (crawler == null) {
            throw new IllegalArgumentException("crawler==null?");
        }
        if (generation == null) {
            throw new IllegalArgumentException("generation==null?");
        }
        if (task == null) {
            throw new IllegalArgumentException("task==null?");
        }
        this.crawler = crawler;
        this.generation = generation;
        this.task = task;
    }

    public LinkCrawlerTask getTask() {
        return task;
    }

    public LinkCrawler getLinkCrawler() {
        return crawler;
    }

    public void run() {
        final LinkCrawlerLock lock = getLinkCrawlerLock();
        if (lock == null || !lock.requiresLocking()) {
            run_now();
        } else {
            run_delayed(lock);
        }
    }

    protected void run_delayed(LinkCrawlerLock lock) {
        final int maxConcurrency = lock.getMaxConcurrency();
        final LinkCrawlerRunnable startRunnable;
        synchronized (SEQ_RUNNABLES) {
            List<LinkCrawlerRunnable> seqs = SEQ_RUNNABLES.get(lock);
            if (seqs == null) {
                /* no queued sequential runnable */
                seqs = new ArrayList<LinkCrawlerRunnable>();
                SEQ_RUNNABLES.put(lock, seqs);
            }
            AtomicInteger counter = SEQ_COUNTER.get(lock);
            if (counter == null) {
                counter = new AtomicInteger(0);
                SEQ_COUNTER.put(lock, counter);
            }
            if (counter.get() < maxConcurrency) {
                /* we have still some slots available for concurrent running */
                if (seqs.size() > 0) {
                    startRunnable = seqs.remove(0);
                    seqs.add(this);
                } else {
                    startRunnable = this;
                }
                counter.incrementAndGet();
            } else {
                startRunnable = null;
                seqs.add(this);
            }
        }
        if (startRunnable == null) {
            return;
        }
        try {
            startRunnable.run_now();
        } finally {
            synchronized (SEQ_RUNNABLES) {
                final List<LinkCrawlerRunnable> seqs = SEQ_RUNNABLES.get(lock);
                final AtomicInteger counter = SEQ_COUNTER.get(lock);
                if (seqs != null) {
                    /* remove current Runnable */
                    counter.decrementAndGet();
                    if (seqs.size() == 0) {
                        if (counter.get() == 0) {
                            /* remove sequential runnable queue */
                            SEQ_RUNNABLES.remove(lock);
                            SEQ_COUNTER.remove(lock);
                        }
                    } else {
                        /* process next waiting runnable */
                        final LinkCrawlerRunnable next = seqs.remove(0);
                        LinkCrawler.threadPool.execute(next);
                    }
                }
            }
        }
    }

    /**
     * run this Runnable now
     */
    protected void run_now() {
        try {
            if (getGeneration().isValid()) {
                crawling();
            }
        } finally {
            LinkCrawler.checkFinishNotify(getTask());
        }
    }

    abstract void crawling();

    public long getAverageRuntime() {
        return 0;
    }

    protected LinkCrawlerLock getLinkCrawlerLock() {
        return null;
    }
}
