package org.jdownloader.captcha.v2.challenge.hcaptcha;

import java.util.ArrayList;

import jd.controlling.captcha.SkipException;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkCollector.JobLinkCrawler;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.controlling.linkcrawler.LinkCrawlerThread;
import jd.http.Browser;
import jd.plugins.CaptchaException;
import jd.plugins.DecrypterException;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.logging2.LogSource;
import org.jdownloader.captcha.blacklist.BlacklistEntry;
import org.jdownloader.captcha.blacklist.BlockAllCrawlerCaptchasEntry;
import org.jdownloader.captcha.blacklist.BlockCrawlerCaptchasByHost;
import org.jdownloader.captcha.blacklist.BlockCrawlerCaptchasByPackage;
import org.jdownloader.captcha.blacklist.CaptchaBlackList;
import org.jdownloader.captcha.v2.CaptchaCrawlerHelperInterface;
import org.jdownloader.captcha.v2.ChallengeResponseController;
import org.jdownloader.captcha.v2.solverjob.SolverJob;

public class CaptchaHelperCrawlerPluginHCaptcha extends AbstractHCaptcha<PluginForDecrypt> implements CaptchaCrawlerHelperInterface {
    public CaptchaHelperCrawlerPluginHCaptcha(final PluginForDecrypt plugin, final Browser br, final String siteKey) {
        super(plugin, br, siteKey);
    }

    public CaptchaHelperCrawlerPluginHCaptcha(final PluginForDecrypt plugin, final Browser br) {
        this(plugin, br, null);
    }

    public String getToken() throws PluginException, InterruptedException, DecrypterException {
        logger.info("SiteDomain:" + getSiteDomain() + "|SiteKey:" + getSiteKey());
        runDdosPrevention();
        if (Thread.currentThread() instanceof SingleDownloadController) {
            logger.severe("PluginForDecrypt.getCaptchaCode inside SingleDownloadController!?");
        }
        if (siteKey == null) {
            siteKey = getSiteKey();
            if (siteKey == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "HCaptcha sitekey can not be found");
            }
        }
        final PluginForDecrypt plugin = getPlugin();
        final HCaptchaChallenge c = createChallenge();
        c.setTimeout(plugin == null ? 60000 : plugin.getChallengeTimeout(c));
        if (plugin != null) {
            plugin.invalidateLastChallengeResponse();
        }
        final BlacklistEntry<?> blackListEntry = CaptchaBlackList.getInstance().matches(c);
        if (blackListEntry != null) {
            logger.warning("Cancel. Blacklist Matching");
            throw new CaptchaException(blackListEntry);
        }
        ArrayList<SolverJob<String>> jobs = new ArrayList<SolverJob<String>>();
        try {
            jobs.add(ChallengeResponseController.getInstance().handle(c));
            if (!c.isSolved()) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            } else if (!c.isCaptchaResponseValid()) {
                final String value = c.getResult().getValue();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Captcha reponse value did not validate:" + value);
            } else {
                return c.getResult().getValue();
            }
        } catch (PluginException e) {
            for (int i = 0; i < jobs.size(); i++) {
                jobs.get(i).invalidate();
            }
            throw e;
        } catch (InterruptedException e) {
            LogSource.exception(logger, e);
            throw e;
        } catch (SkipException e) {
            LogSource.exception(logger, e);
            switch (e.getSkipRequest()) {
            case BLOCK_ALL_CAPTCHAS:
                CaptchaBlackList.getInstance().add(new BlockAllCrawlerCaptchasEntry(plugin.getCrawler()));
                break;
            case BLOCK_HOSTER:
                CaptchaBlackList.getInstance().add(new BlockCrawlerCaptchasByHost(plugin.getCrawler(), plugin.getHost()));
                break;
            case BLOCK_PACKAGE:
                CaptchaBlackList.getInstance().add(new BlockCrawlerCaptchasByPackage(plugin.getCrawler(), plugin.getCurrentLink()));
                break;
            case REFRESH:
                break;
            case TIMEOUT:
                plugin.onCaptchaTimeout(plugin.getCurrentLink(), c);
                // TIMEOUT may fallthrough to SINGLE
            case SINGLE:
                break;
            case STOP_CURRENT_ACTION:
                if (Thread.currentThread() instanceof LinkCrawlerThread) {
                    final LinkCrawler linkCrawler = ((LinkCrawlerThread) Thread.currentThread()).getCurrentLinkCrawler();
                    if (linkCrawler instanceof JobLinkCrawler) {
                        final JobLinkCrawler jobLinkCrawler = ((JobLinkCrawler) linkCrawler);
                        logger.info("Abort JobLinkCrawler:" + jobLinkCrawler.getUniqueAlltimeID().toString());
                        jobLinkCrawler.abort();
                    } else {
                        logger.info("Abort global LinkCollector");
                        LinkCollector.getInstance().abort();
                    }
                    CaptchaBlackList.getInstance().add(new BlockAllCrawlerCaptchasEntry(getPlugin().getCrawler()));
                }
                break;
            default:
                break;
            }
            throw new CaptchaException(e.getSkipRequest());
        } finally {
            c.cleanup();
        }
    }
}
