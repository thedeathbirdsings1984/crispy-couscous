//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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

import java.io.File;
import java.io.IOException;

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "linkz.ge" }, urls = { "https?://(?:www\\.)?linkz\\.ge/file/\\d+/(?:[^<>\"/]+\\.html)?" })
public class LinkzGe extends PluginForHost {
    public LinkzGe(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium(COOKIE_HOST + "/service.php");
    }

    @Override
    public String getAGBLink() {
        return COOKIE_HOST + "/rules.php";
    }

    private static final String COOKIE_HOST     = "http://linkz.ge";
    private static final int    DEFAULTWAITTIME = 0;

    // MhfScriptBasic 1.9
    // FREE limits: 1 * 20
    // PREMIUM limits: Chunks * Maxdl
    // Captchatype: none
    // Other notes:
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("(en|ru|fr|es|de)/file/", "file/"));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie(COOKIE_HOST, "mfh_mylang", "en");
        br.setCookie(COOKIE_HOST, "yab_mylang", "en");
        br.getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404 || br.getURL().contains("&code=DL_FileNotFound") || br.containsHTML("(Your requested file is not found|No file found)")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = getData("File name:");
        String filesize = getData("File size:");
        if (filename == null || filename.matches("")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        link.setFinalFileName(filename.trim());
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        requestFileInformation(link);
        if (br.containsHTML("value=\"Free Users\"")) {
            br.postPage(link.getDownloadURL(), "Free=Free+Users");
        } else if (br.getFormbyProperty("name", "entryform1") != null) {
            br.submitForm(br.getFormbyProperty("name", "entryform1"));
        }
        final Browser ajaxBR = br.cloneBrowser();
        ajaxBR.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        ajaxBR.addAllowedResponseCodes(500);
        final String rcID = br.getRegex("challenge\\?k=([^<>\"]*?)\"").getMatch(0);
        if (rcID != null) {
            final Recaptcha rc = new Recaptcha(br, this);
            rc.setId(rcID);
            rc.load();
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode("recaptcha", cf, link);
            ajaxBR.postPage(link.getDownloadURL(), "downloadverify=1&d=1&recaptcha_response_field=" + c + "&recaptcha_challenge_field=" + rc.getChallenge());
            if (ajaxBR.containsHTML("incorrect\\-captcha\\-sol")) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
        } else if (br.containsHTML(this.getHost() + "/captcha\\.php\"")) {
            final String code = getCaptchaCode("mhfstandard", "/captcha.php?rand=" + System.currentTimeMillis(), link);
            ajaxBR.postPage(link.getDownloadURL(), "downloadverify=1&d=1&captchacode=" + code);
            if (ajaxBR.containsHTML("Captcha number error or expired")) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
        } else {
            ajaxBR.postPage(link.getDownloadURL(), "downloadverify=1&d=1");
        }
        if (ajaxBR.getHttpConnection().getResponseCode() == 500) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Host issue");
        }
        final String reconnectWaittime = ajaxBR.getRegex("You must wait (\\d+) mins\\. for next download.").getMatch(0);
        if (reconnectWaittime != null) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(reconnectWaittime) * 60 * 1001l);
        }
        if (ajaxBR.containsHTML(">You have got max allowed download sessions from the same")) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l);
        }
        final String finalLink = findLink(ajaxBR);
        if (finalLink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        int wait = DEFAULTWAITTIME;
        String waittime = br.getRegex("countdown\\((\\d+)\\);").getMatch(0);
        // Fol older versions it's usually skippable
        // if (waittime == null) waittime =
        // br.getRegex("var timeout=\\'(\\d+)\\';").getMatch(0);
        if (waittime != null) {
            wait = Integer.parseInt(waittime);
        }
        sleep(wait * 1001l, link);
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, finalLink, false, 1);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            if (br.containsHTML(">\\s*AccessKey is expired, please request")) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "FATAL server error, waittime skipped?");
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private String findLink(final Browser br) throws Exception {
        return br.getRegex("(http://[a-z0-9\\-\\.]{5,30}/getfile\\.php\\?id=[A-Za-z0-9]+[^<>\"\\']*?)(\"|\\')").getMatch(0);
    }

    private String getData(final String data) {
        String result = br.getRegex(">" + data + "</strong></li>[\t\n\r ]+<li class=\"col\\-w50\">([^<>\"]*?)</li>").getMatch(0);
        if (result == null) {
            result = br.getRegex("<b>" + data + "</b></td>[\t\n\r ]+<td align=left( width=\\d+px)?>([^<>\"]*?)</td>").getMatch(1);
        }
        return result;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("free"))) {
            /* free accounts also have captchas */
            return true;
        }
        return false;
    }
}