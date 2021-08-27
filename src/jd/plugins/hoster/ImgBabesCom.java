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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.XFileSharingProBasic;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class ImgBabesCom extends XFileSharingProBasic {
    public ImgBabesCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    private static String type_special = "https?://[^/]+/f/([a-z0-9]+)/([^/]+)\\.html";

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: 2020-04-03: null<br />
     * other:<br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "imgbabes.com" });
        return ret;
    }

    public static final String getDefaultAnnotationPatternPartImgbabes() {
        return "(" + XFileSharingProBasic.getDefaultAnnotationPatternPart() + "|/f/[a-z0-9]{10,12}/[^/+]+\\.html)";
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + ImgBabesCom.getDefaultAnnotationPatternPartImgbabes());
        }
        return ret.toArray(new String[0]);
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return ImgBabesCom.buildAnnotationUrls(getPluginDomains());
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return true;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return true;
        } else {
            /* Free(anonymous) and unknown account type */
            return true;
        }
    }

    @Override
    public int getMaxChunks(final Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return 1;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return 1;
        } else {
            /* Free(anonymous) and unknown account type */
            return 1;
        }
    }

    @Override
    public int getMaxSimultaneousFreeAnonymousDownloads() {
        return -1;
    }

    @Override
    public int getMaxSimultaneousFreeAccountDownloads() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    protected boolean isImagehoster() {
        return true;
    }

    @Override
    protected boolean requiresWWW() {
        return true;
    }

    @Override
    protected boolean supports_availablecheck_filename_abuse() {
        return false;
    }

    @Override
    protected boolean supports_availablecheck_alt() {
        return false;
    }

    @Override
    public Form findImageForm(final Browser br) {
        /* 2020-10-22: Special */
        Form imghost_next_form = super.findImageForm(br);
        if (imghost_next_form == null) {
            imghost_next_form = br.getFormbyProperty("id", "myform");
        }
        /* 2020-10-22: Usually 3 seconds waittime */
        final String waitStr = new Regex(correctedBR, "var countdown = (\\d+);").getMatch(0);
        if (waitStr != null) {
            try {
                this.sleep(Long.parseLong(waitStr) * 1001l, this.getDownloadLink());
            } catch (final Throwable e) {
            }
        }
        return imghost_next_form;
    }

    @Override
    protected String getDllinkImagehost(final String src) {
        String dllink = new Regex(correctedBR, "id=\"source\" src=\"(https?://[^/]+/i\\.php\\?[^\";]+)").getMatch(0);
        if (StringUtils.isEmpty(dllink)) {
            /* Fallback to template handling */
            dllink = super.getDllinkImagehost(src);
        }
        return dllink;
    }

    @Override
    protected String getFUID(final String url, URL_TYPE type) {
        // if (url != null && url.matches(type_special)) {
        // return new Regex(url, type_special).getMatch(0);
        // } else {
        // return super.getFUID(url, type);
        // }
        return super.getFUID(url, type);
    }

    @Override
    public String getFUIDFromURL(final DownloadLink link) {
        if (link.getPluginPatternMatcher() != null && link.getPluginPatternMatcher().matches(type_special)) {
            return new Regex(link.getPluginPatternMatcher(), type_special).getMatch(0);
        } else {
            return super.getFUIDFromURL(link);
        }
    }

    @Override
    protected String buildURLPath(DownloadLink link, final String fuid, URL_TYPE type) {
        if (link.getPluginPatternMatcher().matches(type_special)) {
            try {
                return new URL(link.getPluginPatternMatcher()).getPath();
            } catch (final MalformedURLException e) {
                return null;
            }
        } else {
            return super.buildURLPath(link, fuid, type);
        }
    }

    @Override
    public String getFilenameFromURL(final DownloadLink dl) {
        if (dl.getPluginPatternMatcher() != null && dl.getPluginPatternMatcher().matches(type_special)) {
            return new Regex(dl.getPluginPatternMatcher(), type_special).getMatch(1);
        } else {
            return super.getFilenameFromURL(dl);
        }
    }
}