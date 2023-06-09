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
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.requests.GetRequest;
import jd.nutils.encoding.Encoding;
import jd.nutils.encoding.HTMLEntities;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.decrypter.DiskYandexNetFolder;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "disk.yandex.net" }, urls = { "http://yandexdecrypted\\.net/\\d+" })
public class DiskYandexNet extends PluginForHost {
    public DiskYandexNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://passport.yandex.ru/passport?mode=register&from=cloud&retpath=https%3A%2F%2Fdisk.yandex.ru%2F%3Fauth%3D1&origin=face.en");
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "https://disk.yandex.net/";
    }

    /* Settings values */
    private final String         MOVE_FILES_TO_ACCOUNT                 = "MOVE_FILES_TO_ACCOUNT";
    private final String         DELETE_FROM_ACCOUNT_AFTER_DOWNLOAD    = "EMPTY_TRASH_AFTER_DOWNLOAD";
    private static final String  NORESUME                              = "NORESUME";
    /* Some constants which they used in browser */
    public static final String   CLIENT_ID                             = "2784000881613056614464";
    /* Connection limits */
    private final boolean        FREE_RESUME                           = true;
    private final int            FREE_MAXCHUNKS                        = 0;
    private static final int     FREE_MAXDOWNLOADS                     = 20;
    private final boolean        ACCOUNT_FREE_RESUME                   = true;
    private final int            ACCOUNT_FREE_MAXCHUNKS                = 0;
    private static final int     ACCOUNT_FREE_MAXDOWNLOADS             = 20;
    /* Domains & other login stuff */
    private final String[]       cookie_domains                        = new String[] { "https://yandex.ru", "https://yandex.com", "https://disk.yandex.ru/", "https://disk.yandex.com/", "https://disk.yandex.net/", "https://disk.yandex.com.tr/", "https://disk.yandex.kz/" };
    public static final String[] sk_domains                            = new String[] { "disk.yandex.com", "disk.yandex.ru", "disk.yandex.com.tr", "disk.yandex.ua", "disk.yandex.az", "disk.yandex.com.am", "disk.yandex.com.ge", "disk.yandex.co.il", "disk.yandex.kg", "disk.yandex.lt", "disk.yandex.lv", "disk.yandex.md", "disk.yandex.tj", "disk.yandex.tm", "disk.yandex.uz", "disk.yandex.fr", "disk.yandex.ee", "disk.yandex.kz", "disk.yandex.by" };
    /* Properties */
    public static final String   PROPERTY_HASH                         = "hash_main";
    public static final String   PROPERTY_INTERNAL_FUID                = "INTERNAL_FUID";
    public static final String   PROPERTY_QUOTA_REACHED                = "quoty_reached";
    public static final String   PROPERTY_CRAWLED_FILENAME             = "plain_filename";
    public static final String   PROPERTY_PATH_INTERNAL                = "path_internal";
    public static final String   PROPERTY_LAST_AUTH_SK                 = "last_auth_sk";
    public static final String   PROPERTY_LAST_URL                     = "last_url";
    public static final String   PROPERTY_ACCOUNT_ENFORCE_COOKIE_LOGIN = "enforce_cookie_login";
    /*
     * https://tech.yandex.com/disk/api/reference/public-docpage/ 2018-08-09: API(s) seem to work fine again - in case of failure, please
     * disable use_api_file_free_availablecheck ONLY!!
     */
    private static final boolean use_api_file_free_availablecheck      = true;
    private static final boolean use_api_file_free_download            = true;

    /* Make sure we always use our main domain */
    private String getMainLink(final DownloadLink dl) throws Exception {
        String mainlink = dl.getStringProperty("mainlink", null);
        if (mainlink == null && getRawHash(dl) != null) {
            mainlink = String.format("https://yadi.sk/public/?hash=%s", URLEncode.encodeURIComponent(getRawHash(dl)));
        }
        if (mainlink == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (!StringUtils.contains(mainlink, "yadi")) {
            mainlink = "https://disk.yandex.com/" + new Regex(mainlink, "yandex\\.[^/]+/(.+)").getMatch(0);
        }
        return mainlink;
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        if (link.hasProperty(PROPERTY_INTERNAL_FUID)) {
            return this.getHost() + "://" + link.getStringProperty(PROPERTY_INTERNAL_FUID);
        } else {
            return super.getLinkID(link);
        }
    }

    /** Returns currently used domain */
    public static String getCurrentDomain() {
        return "disk.yandex.com";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, null);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account) throws Exception {
        if (getRawHash(link) == null || this.getPath(link) == null) {
            /* This should never happen */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (use_api_file_free_availablecheck) {
            return requestFileInformationAPI(link, account);
        } else {
            return requestFileInformationWebsite(link, account);
        }
    }

    public AvailableStatus requestFileInformationAPI(final DownloadLink link, final Account account) throws Exception {
        prepBR(this.br);
        br.setFollowRedirects(true);
        getPage("https://cloud-api.yandex.net/v1/disk/public/resources?public_key=" + URLEncode.encodeURIComponent(this.getHashWithoutPath(link)) + "&path=" + URLEncode.encodeURIComponent(this.getPath(link)));
        if (apiAvailablecheckIsOffline(br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        this.handleErrorsAPI(link, null);
        /* No error --> No quota reached issue! */
        link.setProperty(PROPERTY_QUOTA_REACHED, false);
        return parseInformationAPIAvailablecheckFiles(this, link, (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString()));
    }

    public AvailableStatus requestFileInformationWebsite(final DownloadLink link, final Account account) throws Exception {
        prepBR(this.br);
        br.setFollowRedirects(true);
        getPage(getMainLink(link));
        if (DiskYandexNetFolder.isOfflineWebsite(this.br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        long filesize_long = -1;
        String final_filename = link.getStringProperty("plain_filename", null);
        String filename = this.br.getRegex("class=\"file-name\" data-reactid=\"[^\"]*?\">([^<>\"]+)<").getMatch(0);
        if (StringUtils.isEmpty(filename)) {
            /* Very unsafe method! */
            final String json = br.getRegex("<script type=\"application/json\"[^>]*id=\"store\\-prefetch\"[^>]*>(.*?)<").getMatch(0);
            if (json != null) {
                filename = PluginJSonUtils.getJson(json, "name");
            }
        }
        String filesize_str = link.getStringProperty("plain_size", null);
        if (filesize_str == null) {
            filesize_str = this.br.getRegex("class=\"item-details__name\">Size:</span> ([^<>\"]+)</div>").getMatch(0);
        }
        if (filesize_str == null) {
            /* Language independent */
            filesize_str = this.br.getRegex("class=\"item-details__name\">[^<>\"]+</span> ([\\d\\.]+ (?:B|KB|MB|GB))</div>").getMatch(0);
        }
        if (filesize_str != null) {
            filesize_str = filesize_str.replace(",", ".");
            filesize_long = SizeFormatter.getSize(filesize_str);
        }
        if (final_filename == null) {
            final_filename = filename;
        }
        /* Important for account download handling */
        if (br.containsHTML("class=\"[^\"]+antifile-sharing\"")) {
            link.setProperty(PROPERTY_QUOTA_REACHED, true);
        } else {
            link.setProperty(PROPERTY_QUOTA_REACHED, false);
        }
        if (final_filename == null && filename != null) {
            link.setFinalFileName(filename);
        } else if (final_filename != null) {
            link.setFinalFileName(final_filename);
        }
        if (filesize_long > -1) {
            link.setDownloadSize(filesize_long);
        }
        return AvailableStatus.TRUE;
    }

    public static AvailableStatus parseInformationAPIAvailablecheckFiles(final Plugin plugin, final DownloadLink dl, final Map<String, Object> entries) throws Exception {
        final long filesize = JavaScriptEngineFactory.toLong(entries.get("size"), -1);
        final String error = (String) entries.get("error");
        final String hash = (String) entries.get("public_key");
        final String filename = (String) entries.get("name");
        final String path = (String) entries.get("path");
        final String md5 = (String) entries.get("md5");
        final String sha256 = (String) entries.get("sha256");
        if (error != null || StringUtils.isEmpty(filename) || StringUtils.isEmpty(hash) || StringUtils.isEmpty(path) || filesize == -1) {
            /* Whatever - our link is probably offline! */
            return AvailableStatus.FALSE;
        }
        if (sha256 != null) {
            // only one hash is stored
            dl.setSha256Hash(sha256);
        }
        if (dl.getHashInfo() == null && md5 != null) {
            // sha256 might be invalid
            dl.setMD5Hash(md5);
        }
        dl.setProperty(PROPERTY_HASH, hash + ":" + path);
        dl.setFinalFileName(filename);
        dl.setProperty(PROPERTY_CRAWLED_FILENAME, filename);
        if (filesize > 0) {
            dl.setVerifiedFileSize(filesize);
        }
        // dl.setDownloadSize(filesize);
        return AvailableStatus.TRUE;
    }

    public static AvailableStatus parseInformationWebsiteAvailablecheckFiles(final Plugin plugin, final DownloadLink dl, final Map<String, Object> entries) throws Exception {
        final Map<String, Object> meta = (Map<String, Object>) entries.get("meta");
        final long filesize = JavaScriptEngineFactory.toLong(meta.get("size"), -1);
        final String filename = (String) entries.get("name");
        // final String path = (String) entries.get("path");
        if (StringUtils.isEmpty(filename) || filesize == -1) {
            /* Whatever - our link is probably offline! */
            return AvailableStatus.FALSE;
        }
        dl.setFinalFileName(filename);
        dl.setProperty(PROPERTY_CRAWLED_FILENAME, filename);
        if (filesize > 0) {
            dl.setVerifiedFileSize(filesize);
        }
        // dl.setDownloadSize(filesize);
        return AvailableStatus.TRUE;
    }

    public static boolean apiAvailablecheckIsOffline(final Browser br) {
        if (br.getHttpConnection().getResponseCode() == 404) {
            return true;
        } else {
            return false;
        }
    }

    public static Browser prepBR(final Browser br) {
        br.getHeaders().put("Accept-Language", "en-us;q=0.7,en;q=0.3");
        br.setCookie("https://disk.yandex.com/", "ys", "");
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(new int[] { 429, 500 });
        return br;
    }

    private void getPage(final String url) throws IOException {
        getPage(this.br, url);
    }

    public static void getPage(final Browser br, final String url) throws IOException {
        br.getPage(url);
        /* 2017-03-30: New */
        final String jsRedirect = br.getRegex("(https?://[^<>\"]+force_show=1[^<>\"]*?)").getMatch(0);
        if (jsRedirect != null) {
            br.getPage(jsRedirect);
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        doFree(link, null);
    }

    public void doFree(final DownloadLink link, final Account account) throws Exception, PluginException {
        if (!this.attemptStoredDownloadurlDownload(link, "directurl", FREE_RESUME, FREE_MAXCHUNKS)) {
            String dllink = null;
            if (isFileDownloadQuotaReached(link)) {
                /*
                 * link is only downloadable via account because the public overall download limit (traffic limit) is exceeded. In this case
                 * the user can only download the link by importing it into his account and downloading it "from there".
                 */
                if (account != null) {
                    /* 2021-03-24: At this moment this function should never get called with available Account object. */
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Public download quota reached: Try again later or add account", 30 * 60 * 1000l);
                }
            }
            PluginException exceptionDuringLinkcheck = null;
            try {
                requestFileInformation(link, account);
                if (use_api_file_free_availablecheck) {
                    /*
                     * 2018-08-09: Randomly found this during testing - this seems to be a good way to easily get downloadlinks PLUS via
                     * this way, it is possible to download files which otherwise require the usage of an account e.g. errormessage
                     * "Download limit reached. You can still save this file to your Yandex.Disk" [And then download it via own account]
                     */
                    dllink = PluginJSonUtils.getJson(br, "file");
                }
            } catch (final PluginException exc) {
                logger.info("Exception happened during availablecheck!");
                exceptionDuringLinkcheck = exc;
            }
            if (StringUtils.isEmpty(dllink)) {
                /**
                 * 2021-02-08: Workaround for error "DiskResourceDownloadLimitExceededError": API request for availablecheck will fail while
                 * download is usually possible!
                 */
                try {
                    if (use_api_file_free_download) {
                        /**
                         * Download API:
                         *
                         * https://cloud-api.yandex.net/v1/disk/public/resources/download?public_key=public_key&path=/
                         */
                        /* Free API download. */
                        getPage("https://cloud-api.yandex.net/v1/disk/public/resources/download?public_key=" + URLEncode.encodeURIComponent(getHashWithoutPath(link)) + "&path=" + URLEncode.encodeURIComponent(this.getPath(link)));
                        this.handleErrorsAPI(link, account);
                        final Map<String, Object> entries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(this.br.toString());
                        dllink = (String) entries.get("href");
                        if (dllink == null) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                    } else {
                        if (StringUtils.isEmpty(dllink)) {
                            /* Free website download */
                            if (use_api_file_free_availablecheck) {
                                this.requestFileInformationWebsite(link, account);
                            }
                            final String sk = getSK(this.br);
                            if (sk == null) {
                                logger.warning("sk in website download handling is null");
                                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                            }
                            br.getHeaders().put("Accept", "*/*");
                            br.getHeaders().put("Content-Type", "text/plain");
                            br.postPageRaw("/public-api-desktop/download-url", String.format("{\"hash\":\"%s\",\"sk\":\"%s\"}", getRawHash(link), sk));
                            handleErrorsFree();
                            dllink = PluginJSonUtils.getJsonValue(br, "url");
                            if (StringUtils.isEmpty(dllink)) {
                                logger.warning("Failed to find final downloadurl");
                                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                            }
                            /* Don't do htmldecode because the link will become invalid then */
                            /* sure json will return url with htmlentities? */
                            dllink = HTMLEntities.unhtmlentities(dllink);
                        }
                    }
                } catch (final Exception exc) {
                    if (exceptionDuringLinkcheck != null) {
                        logger.info("Throwing Exception that happened during linkcheck");
                        throw exceptionDuringLinkcheck;
                    } else {
                        logger.info("Throwing exception that happened while trying to find final downloadurl");
                        throw exc;
                    }
                }
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, FREE_RESUME, FREE_MAXCHUNKS);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                try {
                    br.followConnection(true);
                } catch (final IOException e) {
                    logger.log(e);
                }
                handleServerErrors(link);
                /* This will most likely happen for 0 byte filesize files / serverside broken files. */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown download error");
            }
            link.setProperty("directurl", dl.getConnection().getURL().toString());
        }
        dl.startDownload();
    }

    private void handleErrorsAPI(final DownloadLink link, final Account account) throws PluginException {
        final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        if (entries.containsKey("error")) {
            final String error = (String) entries.get("error");
            final String description = (String) entries.get("description");
            if (error.equalsIgnoreCase("DiskNotFoundError")) {
                /* {"message":"Не удалось найти запрошенный ресурс.","description":"Resource not found.","error":"DiskNotFoundError"} */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (error.equalsIgnoreCase("DiskResourceDownloadLimitExceededError")) {
                if (link != null) {
                    link.setProperty(PROPERTY_QUOTA_REACHED, true);
                }
                if (account == null) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "File has reached quota limit: Wait or add account and retry", 30 * 60 * 1000l);
                } else {
                    /* This should never happen */
                    logger.warning("Single file quota reached although account is given");
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, description, 5 * 60 * 1000l);
                }
            } else {
                /* Handle unknown errors */
                if (link == null) {
                    throw new AccountUnavailableException("Unknown error: " + description, 5 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown error: " + description);
                }
            }
        }
    }

    private void handleErrorsFree() throws PluginException {
        if (br.containsHTML("\"title\":\"invalid ckey\"")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'invalid ckey'", 5 * 60 * 1000l);
        } else if (br.containsHTML("\"code\":21")) {
            /* Happens when we send a very wrong hash - usually shouldn't happen! */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 21 'bad formed path'", 1 * 60 * 60 * 1000l);
        } else if (br.containsHTML("\"code\":69")) {
            /* Usually this does not happen. Happens also if you actually try to download a "premiumonly" link via this method. */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("\"code\":71")) {
            /* Happens when we send a very wrong hash - usually shouldn't happen! */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 71 'Wrong path'", 1 * 60 * 60 * 1000l);
        } else if (br.containsHTML("\"code\":77")) {
            /* Happens when we send a very wrong hash - usually shouldn't happen! */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 77 'resource not found'", 1 * 60 * 60 * 1000l);
        } else if (br.containsHTML("\"code\":88")) {
            /* Happens when we send a wrong hash - usually shouldn't happen! */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 88 'Decryption error'", 10 * 60 * 1000l);
        }
    }

    private String getRawHash(final DownloadLink dl) {
        final String hash = dl.getStringProperty(PROPERTY_HASH, null);
        return hash;
    }

    private String getHashWithoutPath(final DownloadLink dl) {
        return DiskYandexNetFolder.getHashWithoutPath(this.getRawHash(dl));
    }

    /** Returns relative path of the file in relation to {@link #getHashWithoutPath(DownloadLink)}. */
    private String getPath(final DownloadLink dl) {
        return DiskYandexNetFolder.getPathFromHash(this.getRawHash(dl));
    }

    private String getUserID(final Account acc) {
        return acc.getStringProperty("account_userid");
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                /*
                 * Login procedure can redirect multiple times! It should lead us to disk.yandex.com via the referer parameter in our login
                 * URL.
                 */
                br.setFollowRedirects(true);
                /* Always try to re-use cookies to avoid login captchas! */
                final Cookies cookies = account.loadCookies("");
                final Cookies userCookies = account.loadUserCookies();
                /* 2021-02-15: Implemented cookie login for testing purposes */
                if (userCookies != null) {
                    logger.info("Attempting cookie login...");
                    this.setCookies(userCookies);
                    if (this.checkCookies(account)) {
                        /*
                         * Set username by cookie in an attempt to get a unique username because in theory user can enter whatever he wants
                         * when doing cookie-login!
                         */
                        final String usernameByCookie = br.getCookie(br.getURL(), "yandex_login");
                        if (!StringUtils.isEmpty(usernameByCookie)) {
                            account.setUser(usernameByCookie);
                        }
                        return;
                    } else {
                        if (account.hasEverBeenValid()) {
                            throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_expired());
                        } else {
                            throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_invalid());
                        }
                    }
                }
                if (cookies != null) {
                    this.setCookies(cookies);
                    if (!force) {
                        /* Trust cookies */
                        logger.info("Trust cookies without check");
                        return;
                    }
                    if (this.checkCookies(account)) {
                        account.saveCookies(br.getCookies(this.getHost()), "");
                        return;
                    }
                }
                try {
                    logger.info("Performing full login");
                    /* Check if previous login attempt failed and cookie login is enforced. */
                    if (account.getBooleanProperty(PROPERTY_ACCOUNT_ENFORCE_COOKIE_LOGIN, false)) {
                        showCookieLoginInfo();
                        throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_required());
                    }
                    boolean isLoggedIN = false;
                    boolean requiresCaptcha;
                    final Browser ajaxBR = br.cloneBrowser();
                    ajaxBR.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    ajaxBR.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
                    br.getPage("https://passport.yandex.com/auth?from=cloud&origin=disk_landing_web_signin_ru&retpath=https%3A%2F%2Fdisk.yandex.com%2F%3Fsource%3Dlanding_web_signin&backpath=https%3A%2F%2Fdisk.yandex.com");
                    for (int i = 0; i <= 4; i++) {
                        final Form[] forms = br.getForms();
                        if (forms.length == 0) {
                            logger.warning("Failed to find loginform");
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        final Form loginform = forms[0];
                        loginform.remove("twoweeks");
                        loginform.put("source", "password");
                        loginform.put("login", Encoding.urlEncode(account.getUser()));
                        loginform.put("passwd", Encoding.urlEncode(account.getPass()));
                        if (br.containsHTML("\\&quot;captchaRequired\\&quot;:true")) {
                            /** TODO: 2018-08-10: Fix captcha support */
                            /* 2018-04-18: Only required after 10 bad login attempts or bad IP */
                            requiresCaptcha = true;
                            final String csrf_token = loginform.hasInputFieldByName("csrf_token") ? loginform.getInputField("csrf_token").getValue() : null;
                            final String idkey = loginform.hasInputFieldByName("idkey") ? loginform.getInputField("idkey").getValue() : null;
                            if (csrf_token == null || idkey == null) {
                                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                            }
                            ajaxBR.postPage("/registration-validations/textcaptcha", "csrf_token=" + csrf_token + "&track_id=" + idkey);
                            final String url_captcha = PluginJSonUtils.getJson(ajaxBR, "image_url");
                            final String id = PluginJSonUtils.getJson(ajaxBR, "id");
                            if (StringUtils.isEmpty(url_captcha) || StringUtils.isEmpty(id)) {
                                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                            }
                            final DownloadLink dummyLink = new DownloadLink(this, "Account", account.getHoster(), "https://" + account.getHoster(), true);
                            final String c = getCaptchaCode(url_captcha, dummyLink);
                            loginform.put("captcha_answer", c);
                            // loginform.put("idkey", id);
                        } else {
                            requiresCaptcha = false;
                        }
                        br.submitForm(loginform);
                        isLoggedIN = br.getCookie(br.getURL(), "yandex_login", Cookies.NOTDELETEDPATTERN) != null;
                        if (!isLoggedIN) {
                            /* 2021-02-11: Small workaround/test */
                            isLoggedIN = br.getCookie("yandex.com", "yandex_login", Cookies.NOTDELETEDPATTERN) != null;
                        }
                        if (!requiresCaptcha) {
                            /* No captcha -> Only try login once! */
                            break;
                        } else if (requiresCaptcha && i > 0) {
                            /* Probably wrong password and we only allow one captcha attempt. */
                            break;
                        } else if (isLoggedIN) {
                            break;
                        }
                    }
                    if (!isLoggedIN) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                    account.saveCookies(br.getCookies(br.getHost()), "");
                } catch (final PluginException e) {
                    if (e.getLinkStatus() == LinkStatus.ERROR_PLUGIN_DEFECT) {
                        logger.info("Normal login failed -> Enforcing cookie login");
                        account.setProperty(PROPERTY_ACCOUNT_ENFORCE_COOKIE_LOGIN, true);
                        /* Don't display dialog e.g. during linkcheck/download-attempt. */
                        if (this.getDownloadLink() == null && !account.hasEverBeenValid()) {
                            showCookieLoginInfo();
                        }
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Login failed - try cookie login", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw e;
                    }
                }
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private void setCookies(final Cookies cookies) {
        for (final String domain : cookie_domains) {
            br.setCookies(domain, cookies);
        }
        br.setCookies("passport.yandex.com", cookies);
    }

    private boolean checkCookies(final Account account) throws IOException {
        br.getPage("https://passport.yandex.com/profile");
        if (br.containsHTML("mode=logout")) {
            /* Set new cookie timestamp */
            logger.info("Cookie login successful");
            return true;
        } else {
            /* Failed - we have to perform a full login! */
            logger.info("Cookie login failed");
            br.clearCookies(br.getHost());
            return false;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        if (br.getRequest() == null || !br.getURL().contains("client/disk")) {
            getPage("/client/disk/");
        }
        final String userID = PluginJSonUtils.getJson(br, "uid");
        if (StringUtils.isEmpty(userID)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        account.setProperty("account_userid", userID);
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        ai.setStatus("Free Account");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        login(account, false);
        // requestFileInformation(link, account);
        /**
         * 2021-02-10: Use website only as because API linkcheck could throw "quota reached" errors which we do avoid in this handling
         * anyways!
         */
        boolean resume = ACCOUNT_FREE_RESUME;
        int maxchunks = ACCOUNT_FREE_MAXCHUNKS;
        if (link.getBooleanProperty(DiskYandexNet.NORESUME, false)) {
            logger.info("Resume is disabled for this try");
            resume = false;
            /* Allow resume for next try */
            link.setProperty(DiskYandexNet.NORESUME, Boolean.valueOf(false));
        }
        /*
         * In plugins, the "move to trash" setting will be grayed out when disabling the first setting but we can still step into this
         * handling -> Ensure that functionality is consistent with GUI settings!
         */
        final boolean moveIntoAccHandlingActive = this.getPluginConfig().getBooleanProperty(MOVE_FILES_TO_ACCOUNT, false);
        final boolean moveToTrashAfterDownloading = moveIntoAccHandlingActive && getPluginConfig().getBooleanProperty(DELETE_FROM_ACCOUNT_AFTER_DOWNLOAD, false);
        final Browser br2;
        final String authSk;
        if (this.attemptStoredDownloadurlDownload(link, "directlink_account", resume, maxchunks)) {
            br2 = this.br.cloneBrowser();
            /* Very important! */
            if (link.hasProperty(PROPERTY_LAST_URL)) {
                br2.setRequest(new GetRequest(link.getStringProperty(PROPERTY_LAST_URL)));
            }
            authSk = link.getStringProperty(PROPERTY_LAST_AUTH_SK);
        } else {
            requestFileInformationWebsite(link, account);
            final String userID = getUserID(account);
            authSk = PluginJSonUtils.getJson(this.br, "authSk");
            if (authSk == null || userID == null) {
                /* This should never happen */
                logger.warning("authSk or userID is null");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setProperty(PROPERTY_LAST_AUTH_SK, authSk);
            link.setProperty(PROPERTY_LAST_URL, this.br.getURL());
            // final String id0 = diskGetID0(link);
            /*
             * Move files into account and download them "from there" although user might not have selected this? --> Forced handling, only
             * required if not possible via different way
             */
            final boolean downloadableViaAccountOnly = isFileDownloadQuotaReached(link);
            Map<String, Object> entries = null;
            String dllink = null;
            if (!moveIntoAccHandlingActive && !downloadableViaAccountOnly) {
                logger.info("MoveToAccount handling is inactive -> Starting free account download handling");
                br.getHeaders().put("Accept", "*/*");
                br.getHeaders().put("Content-Type", "text/plain");
                br.postPageRaw("/public/api/download-url", URLEncode.encodeURIComponent(String.format("{\"hash\":\"%s\",\"sk\":\"%s\",\"uid\":\"%s\",\"options\":{\"hasExperimentVideoWithoutPreview\":true}}", this.getRawHash(link), authSk, userID)));
                entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                dllink = (String) JavaScriptEngineFactory.walkJson(entries, "data/url");
                if (StringUtils.isEmpty(dllink)) {
                    logger.warning("Failed to find final downloadurl");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            if (moveIntoAccHandlingActive || downloadableViaAccountOnly || StringUtils.isEmpty(dllink)) {
                if (downloadableViaAccountOnly) {
                    logger.info("FORCED moveIntoAccHandling active");
                }
                br.getHeaders().put("Accept", "*/*");
                br.getHeaders().put("Content-Type", "text/plain");
                String internal_file_path = getInternalFilePath(link);
                logger.info("MoveFileIntoAccount: MoveFileIntoAccount handling is active");
                if (internal_file_path == null) {
                    logger.info("MoveFileIntoAccount: No internal filepath available --> Trying to move file into account");
                    /**
                     * 2021-02-10: Possible values for "source": public_web_copy, public_web_copy_limit </br>
                     * public_web_copy_limit is usually used if the files is currently quota limited and cannot be downloaded at all at this
                     * moment. </br>
                     * Both will work but we'll try to choose the one which would also be used via browser.
                     */
                    final String copySource;
                    if (this.isFileDownloadQuotaReached(link)) {
                        copySource = "public_web_copy_limit";
                    } else {
                        copySource = "public_web_copy";
                    }
                    br.postPageRaw("/public/api/save", URLEncode.encodeURIComponent(String.format("{\"hash\":\"%s\",\"name\":\"%s\",\"lang\":\"en\",\"source\":\"%s\",\"isAlbum\":false,\"itemId\":null,\"sk\":\"%s\",\"uid\":\"%s\",\"options\":{\"hasExperimentVideoWithoutPreview\":true}}", this.getRawHash(link), link.getName(), copySource, authSk, userID)));
                    entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                    internal_file_path = (String) JavaScriptEngineFactory.walkJson(entries, "data/path");
                    final String oid = (String) JavaScriptEngineFactory.walkJson(entries, "data/oid");
                    if (br.containsHTML("\"code\":85")) {
                        logger.info("MoveFileIntoAccount: failed to move file to account: No free space available");
                        // throw new PluginException(LinkStatus.ERROR_FATAL, "No free space available, failed to move file to account");
                        throw new AccountUnavailableException("No free space available, failed to move file to account", 5 * 60 * 1000l);
                    } else if (StringUtils.isEmpty(internal_file_path)) {
                        /* This should never happen! */
                        logger.info("MoveFileIntoAccount: Failed to move file into account: WTF");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    } else if (StringUtils.isEmpty(oid)) {
                        /* Should never happen */
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Error while importing file into account: Failed to find oid");
                    }
                    /* Save internal path for later. */
                    link.setProperty(PROPERTY_PATH_INTERNAL, internal_file_path);
                    /* Sometimes the process of 'moving' a file into a cloud account can take some seconds. */
                    logger.info("transfer-status: checking");
                    boolean fileWasImportedSuccessfully = false;
                    for (int i = 1; i < 5; i++) {
                        br.getHeaders().put("Content-Type", "text/plain");
                        br.postPageRaw("https://" + getCurrentDomain() + "/public/api/get-operation-status", URLEncode.encodeURIComponent(String.format("{\"oid\":\"%s\",\"sk\":\"%s\",\"uid\":\"%s\"}", oid, authSk, userID)));
                        final String copyState = PluginJSonUtils.getJson(br, "state");
                        logger.info("Copy state: " + copyState);
                        if (copyState.equalsIgnoreCase("COMPLETED")) {
                            fileWasImportedSuccessfully = true;
                            break;
                        } else if (copyState.equalsIgnoreCase("FAILED")) {
                            logger.info("Possibly failed to copy file to account");
                            break;
                        }
                        sleep(i * 1000l, link);
                    }
                    if (!fileWasImportedSuccessfully) {
                        /* Should never happen */
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Error while importing file into account");
                    }
                } else {
                    logger.info("given/stored internal filepath: " + internal_file_path);
                }
                dllink = getDllinkFromFileInAccount(link, authSk, br);
                if (dllink == null) {
                    logger.warning("MoveFileIntoAccount: Fatal failure - failed to generate downloadurl ");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            /* Small workaround - use stored browser as it contains our originally used host. */
            br2 = this.br.cloneBrowser();
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, maxchunks);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                logger.warning("The final dllink seems not to be a file!");
                try {
                    br.followConnection(true);
                } catch (final IOException e) {
                    logger.log(e);
                }
                handleServerErrors(link);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (link.getFinalFileName() == null) {
                dl.setFilenameFix(true);
            }
            link.setProperty("directlink_account", dllink);
        }
        try {
            dl.startDownload();
        } finally {
            if (moveToTrashAfterDownloading) {
                /*
                 * Don't care whether or not the download was successful as we've stored our generated directURL. If that fails too, we'll
                 * just import the file into the users' account again.
                 */
                moveFileToTrash(br2, link, authSk);
                emptyTrash(br2, authSk);
            }
        }
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link, final String directlinkproperty, final boolean resume, final int maxchunks) throws Exception {
        final String url = link.getStringProperty(directlinkproperty);
        if (url == null) {
            return false;
        }
        try {
            dl = new jd.plugins.BrowserAdapter().openDownload(br, this.getDownloadLink(), url, resume, maxchunks);
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                return true;
            } else {
                throw new IOException();
            }
        } catch (final Throwable e) {
            logger.log(e);
            try {
                dl.getConnection().disconnect();
            } catch (Throwable ignore) {
            }
            return false;
        }
    }

    private boolean isFileDownloadQuotaReached(final DownloadLink dl) {
        return dl.getBooleanProperty(PROPERTY_QUOTA_REACHED, false);
    }

    @SuppressWarnings("deprecation")
    private void handleServerErrors(final DownloadLink link) throws PluginException {
        if (dl.getConnection().getResponseCode() == 403) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403");
        } else if (dl.getConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 30 * 60 * 1000l);
        } else if (dl.getConnection().getResponseCode() == 416) {
            logger.info("Resume impossible, disabling it for the next try");
            link.setChunksProgress(null);
            link.setProperty(DiskYandexNet.NORESUME, Boolean.valueOf(true));
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
    }

    private String getDllinkFromFileInAccount(final DownloadLink dl, final String authSk, final Browser br2) {
        final String filepath = getInternalFilePath(dl);
        if (filepath == null) {
            logger.info("Debug-info: filepath == null, can't throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); here");
            return null;
        }
        try {
            this.br.postPage("https://" + getCurrentDomain() + "/models/?_m=do-get-resource-url", "_model.0=do-get-resource-url&id.0=" + Encoding.urlEncode(filepath) + "&idClient=" + CLIENT_ID + "&sk=" + authSk);
            String dllink = PluginJSonUtils.getJsonValue(br, "file");
            if (!StringUtils.isEmpty(dllink) && dllink.startsWith("//")) {
                /* 2018-04-18 */
                dllink = "https:" + dllink;
            }
            return dllink;
        } catch (final Throwable e) {
            logger.warning("Failed to create dllink of link in account - Exception!");
        }
        return null;
    }

    private void moveFileToTrash(final Browser br2, final DownloadLink dl, final String authSk) {
        final String filepath = getInternalFilePath(dl);
        if (!StringUtils.isEmpty(filepath) && !StringUtils.isEmpty(authSk) && br2.getRequest() != null) {
            logger.info("Trying to move file to trash: " + filepath);
            try {
                br2.postPage("/models/?_m=do-resource-delete", "_model.0=do-resource-delete&id.0=" + Encoding.urlEncode(filepath) + "&idClient=" + CLIENT_ID + "&sk=" + authSk);
                final Map<String, Object> entries = JSonStorage.restoreFromString(br2.toString(), TypeRef.HASHMAP);
                if (entries.containsKey("error")) {
                    logger.info("Possible failure on moving file into trash");
                } else {
                    logger.info("Successfully moved file into trash");
                    dl.removeProperty(PROPERTY_PATH_INTERNAL);
                }
            } catch (final Throwable e) {
                logger.log(e);
                logger.warning("Failed to move file to trash - Exception!");
            }
        } else {
            logger.info("Cannot move any file to trash as there is no stored internal path or authSk available");
        }
    }

    /**
     * Returns URL-encoded internal path for download/move/file POST-requests. This is the path which a specific file has inside a users'
     * account e.g. after importing a public file into an account.
     */
    private String getInternalFilePath(final DownloadLink dl) {
        String filepath = null;
        final boolean newWay = true;
        if (newWay) {
            /* 2018-04-18: New */
            filepath = dl.getStringProperty(PROPERTY_PATH_INTERNAL, null);
        } else {
            final String plain_filename = dl.getStringProperty(PROPERTY_CRAWLED_FILENAME, null);
            filepath = Encoding.urlEncode(plain_filename);
            if (filepath == null) {
                logger.info("Debug-info: filepath == null, can't throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); here");
            }
        }
        return filepath;
    }

    /** Deletes all items inside users' Yandex trash folder. */
    private void emptyTrash(final Browser br2, final String authSk) {
        if (!StringUtils.isEmpty(authSk) && br2.getRequest() != null) {
            try {
                logger.info("Trying to empty trash");
                br2.postPage("/models/?_m=do-clean-trash", "_model.0=do-clean-trash&idClient=" + CLIENT_ID + "&sk=" + authSk);
                logger.info("Successfully emptied trash");
            } catch (final Throwable e) {
                logger.warning("Failed to empty trash");
            }
        } else {
            logger.info("Cannot empty trash as authSk is null or no browser-request is available");
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_FREE_MAXDOWNLOADS;
    }

    public static String getSK(final Browser br) {
        return PluginJSonUtils.getJsonValue(br, "sk");
    }

    /** Gets new 'SK' value via '/auth/status' request. */
    public static String getNewSK(final Browser br, final String domain, final String sourceURL) throws IOException {
        br.getPage("https://" + domain + "/auth/status?urlOrigin=" + Encoding.urlEncode(sourceURL) + "&source=album_web_signin");
        return PluginJSonUtils.getJsonValue(br, "sk");
    }

    @Override
    public String getDescription() {
        return "JDownloader's Yandex.net Plugin helps downloading files from Yandex.net. It provides some settings for downloads via account.";
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Account settings:"));
        final ConfigEntry moveFilesToAcc = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), MOVE_FILES_TO_ACCOUNT, "1. Move files to account before downloading them to get higher download speeds?").setDefaultValue(false);
        getConfig().addEntry(moveFilesToAcc);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), DELETE_FROM_ACCOUNT_AFTER_DOWNLOAD, "2. Delete moved files & empty trash after downloadlink-generation?").setEnabledCondidtion(moveFilesToAcc, true).setDefaultValue(false));
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}