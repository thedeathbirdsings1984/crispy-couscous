//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.hoster;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.MultiHosterManagement;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "filebit.pl" }, urls = { "" })
public class FileBitPl extends PluginForHost {
    private static final String          APIKEY    = "YWI3Y2E2NWM3OWQxYmQzYWJmZWU3NTRiNzY0OTM1NGQ5ODI3ZjlhNmNkZWY3OGE1MjQ0ZjU4NmM5NTNiM2JjYw==";
    private static final String          API_BASE  = "https://filebit.pl/api/index.php";
    private String                       sessionID = null;
    /*
     * 2018-02-13: Their API is broken and only returns 404. Support did not respond which is why we now have website- and API support ...
     */
    private static final boolean         USE_API   = true;
    private static MultiHosterManagement mhm       = new MultiHosterManagement("filebit.pl");

    public FileBitPl(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://filebit.pl/oferta");
    }

    @Override
    public String getAGBLink() {
        return "https://filebit.pl/regulamin";
    }

    private Browser newBrowserAPI() {
        br = new Browser();
        br.setCookiesExclusive(true);
        br.getHeaders().put("Accept", "application/json");
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setCustomCharset("utf-8");
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        br.setAllowedResponseCodes(new int[] { 401, 204, 403, 404, 497, 500, 503 });
        return br;
    }

    private Browser newBrowserWebsite() {
        br = new Browser();
        br.setCookiesExclusive(true);
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setCustomCharset("utf-8");
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws PluginException {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        if (account == null) {
            /* without account its not possible to download the link */
            return false;
        }
        return true;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        /* this should never be called */
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    private void handleDL(final Account account, final DownloadLink link, final String dllink) throws Exception {
        /* we want to follow redirects in final stage */
        br.setFollowRedirects(true);
        br.setCurrentURL(null);
        final int maxChunks = (int) account.getLongProperty("maxconnections", 1);
        link.setProperty("filebitpldirectlink", dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, maxChunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            if (dl.getConnection().getResponseCode() == 403) {
                mhm.handleErrorGeneric(account, link, "403dlerror", 20);
            }
            if (br.containsHTML("<title>FileBit\\.pl \\- Error</title>")) {
                mhm.handleErrorGeneric(account, link, "dlerror_known_but_unsure", 20);
            }
            mhm.handleErrorGeneric(account, link, "dlerror_unknown", 50);
        }
        this.dl.startDownload();
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.MULTIHOST };
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        mhm.runCheck(account, link);
        /*
         * 2019-08-17: It is especially important to try to re-use generated downloadurls in this case because they will charge the complete
         * traffic of a file once you add an URL to their download-queue.
         */
        String dllink = checkDirectLink(link, "filebitpldirectlink");
        if (StringUtils.isEmpty(dllink)) {
            /* request Download */
            dllink = getDllink(account, link);
            if (StringUtils.isEmpty(dllink)) {
                mhm.handleErrorGeneric(account, link, "dllinknull", 20);
            }
        }
        handleDL(account, link, dllink);
    }

    private String getDllink(final Account account, final DownloadLink link) throws Exception {
        final String dllink;
        if (USE_API) {
            dllink = getDllinkAPI(account, link);
        } else {
            dllink = getDllinkWebsite(account, link);
        }
        return dllink;
    }

    private String getDllinkAPI(final Account account, final DownloadLink link) throws Exception {
        this.loginAPI(account);
        long total_numberof_waittime_loops_for_current_fileID = 0;
        final long max_total_numberof_waittime_loops_for_current_fileID = 200;
        String fileID = link.getStringProperty("filebitpl_fileid", null);
        if (!StringUtils.isEmpty(fileID)) {
            /*
             * 2019-08-19: If a downloadlink has previously been created successfully with that fileID and upper handling fails to re-use
             * that generated directURL, re-using the old fileID to generate a new downloadurl will not use up any traffic of the users'
             * account either!!
             */
            logger.info("Found saved fileID:  + fileID");
            logger.info("--> We've tried to download this URL before --> Trying to re-use old fileID as an attempt not to waste any traffic (this multihoster has a bad traffic-counting-system)");
            total_numberof_waittime_loops_for_current_fileID = link.getLongProperty("filebitpl_fileid_total_numberof_waittime_loops", 0);
            logger.info("Stored fileID already went through serverside-download-loops in the past for x-times: " + total_numberof_waittime_loops_for_current_fileID);
        } else {
            /* This initiates a serverside download. The user will be charged for the full amount of traffic immediately! */
            logger.info("Failed to find any old fileID --> Initiating a new serverside queue-download");
            br.getPage(API_BASE + "?a=addNewFile&sessident=" + sessionID + "&url=" + Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, this)));
            handleAPIErrors(br, account, link);
            /* Serverside fileID which refers to the current serverside download-process. */
            fileID = PluginJSonUtils.getJson(br, "fileId");
            if (StringUtils.isEmpty(fileID)) {
                /* This should never happen */
                logger.warning("Failed to find fileID");
                mhm.handleErrorGeneric(account, link, "fileidnull", 20);
            }
            /* Save this ID so that in case of an error or aborted downloads we can re-use it without wasting the users' traffic. */
            link.setProperty("filebitpl_fileid", fileID);
        }
        int loop = 0;
        int lastProgressValue = 0;
        int timesWithoutProgressChange = 0;
        final int timesWithoutProgressChangeMax = 10;
        boolean serverDownloadFinished = false;
        try {
            do {
                br.getPage(API_BASE + "?a=checkFileStatus&sessident=" + sessionID + "&fileId=" + fileID);
                final String error = PluginJSonUtils.getJson(br, "error");
                final String errno = PluginJSonUtils.getJson(br, "errno");
                if ("1".equals(error)) {
                    if ("207".equals(errno)) {
                        mhm.putError(account, link, 30 * 60 * 1000l, "Not enough traffic");
                    } else if ("213".equals(errno)) {
                        mhm.putError(account, link, 1 * 60 * 1000l, "Daily limit reached");
                    }
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final String status = PluginJSonUtils.getJson(br, "status");
                serverDownloadFinished = "3".equals(status);
                if (serverDownloadFinished) {
                    break;
                } else {
                    /* Progress is only available when status == 2 */
                    int progressTemp = 0;
                    String progressStr = PluginJSonUtils.getJson(br, "progress");
                    String info = PluginJSonUtils.getJson(br, "info");
                    if (StringUtils.isEmpty(info)) {
                        info = "File is in queue to be prepared";
                    }
                    info += ": %s%%";
                    if (StringUtils.isEmpty(progressStr)) {
                        progressStr = "0";
                    }
                    if (progressStr.matches("\\d+")) {
                        progressTemp = Integer.parseInt(progressStr);
                    }
                    if (progressTemp == lastProgressValue) {
                        timesWithoutProgressChange++;
                        logger.info("No progress change in loop number: " + timesWithoutProgressChange);
                    } else {
                        lastProgressValue = progressTemp;
                        /* Reset counter */
                        timesWithoutProgressChange = 0;
                    }
                    info = String.format(info, progressStr);
                    sleep(5000l, link, info);
                    loop++;
                }
                if (timesWithoutProgressChange >= timesWithoutProgressChangeMax) {
                    logger.info("No progress change in " + timesWithoutProgressChangeMax + " loops: Giving up");
                    break;
                }
                logger.info("total_numberof_waittime_loops_for_current_fileID: " + total_numberof_waittime_loops_for_current_fileID);
                total_numberof_waittime_loops_for_current_fileID++;
            } while (!serverDownloadFinished && loop <= 200);
        } finally {
            if (total_numberof_waittime_loops_for_current_fileID >= max_total_numberof_waittime_loops_for_current_fileID) {
                /*
                 * Too many attempts for current fileID --> Next retry will create a new fileID which will charge the full amount of traffic
                 * for this file (again).
                 */
                logger.info("Reached max_total_numberof_waittime_loops_for_current_fileID --> Resetting fileid and filebitpl_fileid_total_numberof_waittime_loops for future retries");
                logger.info("Next attempt will charge traffic again");
                link.removeProperty("filebitpl_fileid");
                link.removeProperty("filebitpl_fileid_total_numberof_waittime_loops");
            } else if (loop > 0) {
                /*
                 * Only display this logger and save filebitpl_fileid_total_numberof_waittime_loops if we did over 0 loops for this
                 * download-attempt!
                 */
                logger.info("Current fileID went through this many loops: " + total_numberof_waittime_loops_for_current_fileID);
                link.setProperty("filebitpl_fileid_total_numberof_waittime_loops", total_numberof_waittime_loops_for_current_fileID);
            }
        }
        if (!serverDownloadFinished) {
            /* Rare case */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server transfer retry count exhausted", 5 * 60 * 1000l);
        }
        // final String expires = getJson("expires");
        return PluginJSonUtils.getJson(this.br, "downloadurl");
    }

    /**
     * 2019-08-19: Keep in mind: This may waste traffic as it is not (yet) able to re-use previously generated "filebit.pl fileIDs".</br> DO
     * NOT USE THIS AS LONG AS THEIR API IS WORKING FINE!!!
     */
    private String getDllinkWebsite(final Account account, final DownloadLink link) throws Exception {
        this.loginWebsite(account);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        // do not change to new method, admin is working on new api
        br.postPage("/includes/ajax.php", "a=serverNewFile&url=" + Encoding.urlEncode(link.getDownloadURL()) + "&t=" + System.currentTimeMillis());
        final List<Object> ressourcelist = (List<Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
        Map<String, Object> entries = (Map<String, Object>) ressourcelist.get(0);
        entries = (Map<String, Object>) entries.get("array");
        // final String downloadlink_expires = (String) entries.get("expire");
        // final String internal_id = Long.toString(JavaScriptEngineFactory.toLong(entries.get("id"), 0));
        return (String) entries.get("download");
    }

    private String checkDirectLink(final DownloadLink link, final String property) {
        String dllink = link.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    return dllink;
                }
            } catch (final Exception e) {
                logger.log(e);
                return null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return null;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        this.br = newBrowserAPI();
        if (USE_API) {
            return fetchAccountInfoAPI(account);
        } else {
            return fetchAccountInfoWebsite(account);
        }
    }

    public AccountInfo fetchAccountInfoAPI(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        loginAPI(account);
        br.getPage(API_BASE + "?a=accountStatus&sessident=" + sessionID);
        handleAPIErrors(br, account, null);
        account.setConcurrentUsePossible(true);
        final String accountDescription = PluginJSonUtils.getJson(br, "acctype");
        String premium = PluginJSonUtils.getJson(this.br, "premium");
        final String expire = PluginJSonUtils.getJson(this.br, "expires");
        if (expire != null) {
            final Long expirelng = Long.parseLong(expire);
            if (expirelng == -1) {
                ai.setValidUntil(expirelng);
            } else {
                ai.setValidUntil(System.currentTimeMillis() + expirelng);
            }
        }
        final String trafficleft_bytes = PluginJSonUtils.getJson(this.br, "transferLeft");
        if (trafficleft_bytes != null) {
            ai.setTrafficLeft(trafficleft_bytes);
        } else {
            ai.setUnlimitedTraffic();
        }
        int maxSimultanDls = Integer.parseInt(PluginJSonUtils.getJson(this.br, "maxsin"));
        if (maxSimultanDls < 1) {
            maxSimultanDls = 1;
        } else if (maxSimultanDls > 20) {
            maxSimultanDls = 0;
        }
        account.setMaxSimultanDownloads(maxSimultanDls);
        long maxChunks = Integer.parseInt(PluginJSonUtils.getJson(this.br, "maxcon"));
        if (maxChunks > 1) {
            maxChunks = -maxChunks;
        }
        account.setProperty("maxconnections", maxChunks);
        br.getPage(API_BASE + "?a=getHostList");
        handleAPIErrors(br, account, null);
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        final String[] hostDomains = br.getRegex("\"hostdomains\":\\[(.*?)\\]").getColumn(0);
        for (final String domains : hostDomains) {
            final String[] realDomains = new Regex(domains, "\"(.*?)\"").getColumn(0);
            for (final String realDomain : realDomains) {
                supportedHosts.add(realDomain);
            }
        }
        if (!StringUtils.isEmpty(accountDescription)) {
            account.setType(AccountType.FREE);
            ai.setStatus(accountDescription);
        } else {
            if (!"1".equals(premium)) {
                account.setType(AccountType.FREE);
                ai.setStatus("Free Account");
            } else {
                account.setType(AccountType.PREMIUM);
                ai.setStatus("Premium Account");
            }
        }
        ai.setMultiHostSupport(this, supportedHosts);
        return ai;
    }

    public AccountInfo fetchAccountInfoWebsite(final Account account) throws Exception {
        newBrowserWebsite();
        final AccountInfo ai = new AccountInfo();
        loginWebsite(account);
        br.getPage("/wykaz");
        account.setConcurrentUsePossible(true);
        final boolean isPremium = br.containsHTML("KONTO <span>PREMIUM</span>");
        if (!isPremium) {
            final String lang = System.getProperty("user.language");
            if ("de".equalsIgnoreCase(lang)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nNicht unterstützter Accounttyp!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUnsupported account type!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        long timeleftMilliseconds = 0;
        final Regex timeleftHoursMinutes = br.getRegex("class=\"name\">(\\d+) godzin, (\\d+) minut</p>");
        final String timeleftDays = br.getRegex("<span class=\"long\" style=\"[^<>\"]+\">(\\d+) DNI</span>").getMatch(0);
        final String timeleftHours = timeleftHoursMinutes.getMatch(0);
        final String timeleftMinutes = timeleftHoursMinutes.getMatch(1);
        if (timeleftDays != null) {
            timeleftMilliseconds += Long.parseLong(timeleftDays) * 24 * 60 * 60 * 1000;
        }
        if (timeleftHours != null) {
            timeleftMilliseconds += Long.parseLong(timeleftHours) * 60 * 60 * 1000;
        }
        if (timeleftMinutes != null) {
            timeleftMilliseconds += Long.parseLong(timeleftMinutes) * 60 * 1000;
        }
        if (timeleftMilliseconds > 0) {
            ai.setValidUntil(System.currentTimeMillis() + timeleftMilliseconds, this.br);
        }
        final Regex trafficRegex = br.getRegex("Pobrano dzisiaj:<br />\\s*?<strong style=\"[^\"]*?\">([^<>\"]+) z <span style=\"[^<>\"]*?\">([^<>\"]+)</span>");
        final String trafficUsedTodayStr = trafficRegex.getMatch(0);
        final String traffixMaxTodayStr = trafficRegex.getMatch(1);
        if (trafficUsedTodayStr != null && traffixMaxTodayStr != null) {
            final long trafficMaxToday = SizeFormatter.getSize(traffixMaxTodayStr);
            ai.setTrafficLeft(trafficMaxToday - SizeFormatter.getSize(trafficUsedTodayStr));
            ai.setTrafficMax(trafficMaxToday);
        } else {
            /* This is wrong but we do not want our plugin to break just because of some missing information. */
            ai.setUnlimitedTraffic();
        }
        if (!isPremium) {
            account.setType(AccountType.FREE);
            ai.setStatus("Free Account");
        } else {
            account.setType(AccountType.PREMIUM);
            ai.setStatus("Premium Account");
        }
        final String hostTableText = br.getRegex("<ul class=\"wykazLista\">(.*?)</ul>").getMatch(0);
        if (hostTableText == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        final String[] hostInfo = hostTableText.split("<li>");
        for (final String singleHostInfo : hostInfo) {
            String host = new Regex(singleHostInfo, "<b>([^<>\"]+)</b>").getMatch(0);
            final boolean isActive = singleHostInfo.contains("online.png");
            if (StringUtils.isEmpty(host) || !isActive) {
                continue;
            }
            host = host.toLowerCase();
            supportedHosts.add(host);
        }
        ai.setMultiHostSupport(this, supportedHosts);
        return ai;
    }

    // private void login(final Account account) throws IOException, PluginException, InterruptedException {
    // if (USE_API) {
    // loginAPI(account);
    // } else {
    // loginWebsite(account);
    // }
    // }
    private void loginAPI(final Account account) throws IOException, PluginException, InterruptedException {
        synchronized (account) {
            try {
                newBrowserAPI();
                sessionID = account.getStringProperty("sessionid");
                final long session_expire = account.getLongProperty("sessionexpire", 0);
                if (StringUtils.isEmpty(sessionID) || System.currentTimeMillis() > session_expire) {
                    if (!StringUtils.isEmpty(sessionID)) {
                        try {
                            br.getPage(API_BASE + "?a=accountStatus&sessident=" + sessionID);
                            handleAPIErrors(br, account, null);
                            sessionID = account.getStringProperty("sessionid");
                            if (!StringUtils.isEmpty(sessionID)) {
                                logger.info("Validated stored sessionID");
                                account.setProperty("sessionexpire", System.currentTimeMillis() + 40 * 60 * 60 * 1000);
                                return;
                            }
                        } catch (PluginException e) {
                            logger.log(e);
                        }
                    }
                    logger.info("Performing full login");
                    br.getPage(API_BASE + "?a=login&apikey=" + Encoding.Base64Decode(APIKEY) + "&login=" + Encoding.urlEncode(account.getUser()) + "&password=" + JDHash.getMD5(account.getPass()));
                    handleAPIErrors(br, account, null);
                    sessionID = PluginJSonUtils.getJson(this.br, "sessident");
                    if (StringUtils.isEmpty(sessionID)) {
                        if (br.getHttpConnection().getResponseCode() == 500) {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                        } else {
                            /* This should never happen */
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                        }
                    }
                    /* According to API documentation, sessionIDs are valid for 60 minutes */
                    account.setProperty("sessionexpire", System.currentTimeMillis() + 40 * 60 * 60 * 1000);
                    account.setProperty("sessionid", sessionID);
                } else {
                    logger.info("Trusting stored sessionID");
                }
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    dumpSessionID(account);
                }
                throw e;
            }
        }
    }

    private void dumpSessionID(final Account account) {
        account.removeProperty("sessionid");
    }

    private boolean isLoggedinHTMLWebsite() {
        return br.containsHTML("class=\"wyloguj\"");
    }

    private void loginWebsite(final Account account) throws IOException, PluginException, InterruptedException {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                final Cookies cookies = account.loadCookies("");
                boolean loggedinViaCookies = false;
                if (cookies != null) {
                    /* Avoid full login whenever possible as it requires a captcha to be solved ... */
                    br.setCookies(account.getHoster(), cookies);
                    br.getPage("https://" + account.getHoster() + "/");
                    loggedinViaCookies = isLoggedinHTMLWebsite();
                }
                if (!loggedinViaCookies) {
                    br.getPage("https://" + account.getHoster() + "/");
                    Form loginform = null;
                    final Form[] forms = br.getForms();
                    for (final Form aForm : forms) {
                        if (aForm.containsHTML("/panel/login")) {
                            loginform = aForm;
                            break;
                        }
                    }
                    if (loginform == null) {
                        logger.warning("Failed to find loginform");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    loginform.put("login", Encoding.urlEncode(account.getUser()));
                    loginform.put("password", Encoding.urlEncode(account.getPass()));
                    String reCaptchaKey = br.getRegex("\\'sitekey\\'\\s*?:\\s*?\\'([^<>\"\\']+)\\'").getMatch(0);
                    if (reCaptchaKey == null) {
                        /* 2018-02-13: Fallback-key */
                        reCaptchaKey = "6Lcu5AcUAAAAAC9Hkb6eFqM2P_YLMbI39eYi7KUm";
                    }
                    final DownloadLink dlinkbefore = this.getDownloadLink();
                    if (dlinkbefore == null) {
                        this.setDownloadLink(new DownloadLink(this, "Account", this.getHost(), "https://" + account.getHoster(), true));
                    }
                    final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br, reCaptchaKey).getToken();
                    if (dlinkbefore != null) {
                        this.setDownloadLink(dlinkbefore);
                    }
                    loginform.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                    br.submitForm(loginform);
                    final String redirect = br.getRegex("<meta http\\-equiv=\"refresh\" content=\"\\d+;URL=(http[^<>\"]+)\" />").getMatch(0);
                    if (redirect != null) {
                        br.getPage(redirect);
                    }
                    sessionID = this.br.getCookie(this.br.getHost(), "PHPSESSID");
                    if (sessionID == null || !isLoggedinHTMLWebsite()) {
                        // This should never happen
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(br.getCookies(account.getHoster()), "");
            } catch (PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private void handleAPIErrors(final Browser br, final Account account, final DownloadLink link) throws PluginException, InterruptedException {
        String statusCode = br.getRegex("\"errno\"\\s*:\\s*(\\d+)").getMatch(0);
        if (statusCode == null && br.containsHTML("\"result\":true")) {
            statusCode = "999";
        } else if (statusCode == null) {
            statusCode = "0";
        }
        String statusMessage = PluginJSonUtils.getJson(br, "error");
        try {
            /* Should never happen: {"result":false,"errno":99,"error":"function not found"} */
            int status = Integer.parseInt(statusCode);
            switch (status) {
            case 0:
                /* Everything ok */
                break;
            case 99:
                /* Function not found (should never happen) */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            case 100:
                dumpSessionID(account);
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            case 200:
                /* SessionID expired --> Refresh on next full login */
                statusMessage = "Invalid sessionID";
                dumpSessionID(account);
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            case 201:
                /* MOCH server maintenance */
                statusMessage = "Server maintenance";
                mhm.handleErrorGeneric(account, link, "server_maintenance", 10);
            case 202:
                /* Login/PW missing (should never happen) */
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 203:
                if (StringUtils.containsIgnoreCase(statusMessage, "Twoje IP zostalo zablokowane")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                /* Invalid API key (should never happen) */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            case 204:
                /* Login/PW wrong or account temporary blocked (should never happen) */
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 207:
                /* Custom API error */
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            case 210:
                /* Link offline */
                statusMessage = "Link offline";
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            case 211:
                /* Host not supported -> Remove it from hostList */
                statusMessage = "Host not supported";
                mhm.handleErrorGeneric(account, link, "hoster_unsupported", 5);
            case 212:
                /* Host offline -> Disable for 5 minutes */
                statusMessage = "Host offline";
                mhm.handleErrorGeneric(account, link, "hoster_offline", 5);
            case 213:
                /* one day hosting links count limit reached. */
                /* Przekroczyłeś dzienny limit ilości pobranych plików z tego hostingu. Spróbuj z linkami z innego hostingu. */
                statusMessage = "Daily limit reached";
                mhm.handleErrorGeneric(account, link, "daily_limit_reached", 1);
            default:
                /* unknown error, do not try again with this multihoster */
                statusMessage = "Unknown API error code, please inform JDownloader Development Team";
                mhm.handleErrorGeneric(account, link, "unknown_api_error", 20);
            }
        } catch (final PluginException e) {
            logger.info("Exception: statusCode: " + statusCode + " statusMessage: " + statusMessage);
            throw e;
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}