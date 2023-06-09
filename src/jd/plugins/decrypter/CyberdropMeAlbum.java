//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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
package jd.plugins.decrypter;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.net.URLHelper;
import org.jdownloader.plugins.controller.UpdateRequiredClassNotFoundException;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.http.requests.GetRequest;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class CyberdropMeAlbum extends PluginForDecrypt {
    public CyberdropMeAlbum(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "cyberdrop.me", "cyberdrop.to", "cyberdrop.cc" });
        ret.add(new String[] { "bunkr.su", "bunkr.ru", "bunkr.is" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    private static final String EXTENSIONS = "(?:mp4|m4v|mp3|mov|jpe?g|zip|rar|png|gif|ts|[a-z0-9]{3})";

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            String regex = "https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/a/[A-Za-z0-9]+";
            regex += "|https?://files\\." + buildHostsPatternPart(domains) + "/(?:v|d)/[^/]+\\." + EXTENSIONS;
            regex += "|https?://stream\\d*\\." + buildHostsPatternPart(domains) + "/(?:v|d)/[^/]+\\." + EXTENSIONS;
            regex += "|https?://cdn\\d*\\." + buildHostsPatternPart(domains) + "/[^/]+\\." + EXTENSIONS;
            regex += "|https?://media-files\\d*\\." + buildHostsPatternPart(domains) + "/[^/]+\\." + EXTENSIONS;
            regex += "|https?://fs-\\d+\\." + buildHostsPatternPart(domains) + "/.+\\." + EXTENSIONS;
            ret.add(regex);
        }
        return ret.toArray(new String[0]);
    }

    public static final String TYPE_ALBUM       = "(?i)https?://[^/]+/a/([A-Za-z0-9]+)";                                 // album
    public static final String TYPE_FILES       = "(?i)https?://files\\.[^/]+/(?:v|d)/[^/(.+\\." + EXTENSIONS + ")";     // bunkr
    public static final String TYPE_STREAM      = "(?i)https?://stream(\\d*)\\.[^/]+/(?:v|d)/(.+\\." + EXTENSIONS + ")"; // bunkr
    public static final String TYPE_CDN         = "(?i)https?://cdn(\\d*)\\.[^/]+/(.+\\." + EXTENSIONS + ")";            // bunkr
    public static final String TYPE_FS          = "(?i)https?://fs-(\\d+)\\.[^/]+/(.+\\." + EXTENSIONS + ")";            // cyberdrop
    public static final String TYPE_MEDIA_FILES = "(?i)https?://media-files(\\d*)\\.[^/]+/(.+\\." + EXTENSIONS + ")";    // bunkr
    private PluginForHost      plugin           = null;

    private DownloadLink add(final List<DownloadLink> ret, Set<String> dups, final String directurl, final String filename, final String filesizeBytes, final String filesize) throws Exception {
        if (dups == null || dups.add(directurl)) {
            final String correctedDirectURL = correctDirecturl(directurl);
            final DownloadLink dl;
            if (correctedDirectURL != null) {
                dl = this.createDownloadlink(correctedDirectURL);
            } else {
                dl = this.createDownloadlink(directurl);
            }
            dl.setProperty(DirectHTTP.PROPERTY_RATE_LIMIT, 500);
            dl.setProperty(DirectHTTP.PROPERTY_MAX_CONCURRENT, 1);
            if (plugin == null) {
                try {
                    final LazyHostPlugin lazyHostPlugin = HostPluginController.getInstance().get(getHost());
                    if (lazyHostPlugin == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    } else {
                        plugin = lazyHostPlugin.getPrototype(null, false);
                    }
                } catch (UpdateRequiredClassNotFoundException e) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, null, e);
                }
            }
            if (correctedDirectURL != null) {
                // direct assign the dedicated hoster plugin because it does not have any URL regex
                dl.setDefaultPlugin(plugin);
            }
            dl.setAvailable(true);
            if (filename != null) {
                dl.setFinalFileName(filename);
            }
            if (filesizeBytes != null) {
                dl.setVerifiedFileSize(Long.parseLong(filesizeBytes));
            } else if (filesize != null) {
                dl.setDownloadSize(SizeFormatter.getSize(filesize));
            }
            ret.add(dl);
            return dl;
        } else {
            return null;
        }
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String correctedDirectURL = correctDirecturl(param.getCryptedUrl());
        if (correctedDirectURL != null) {
            // only if domain already contains specific server number
            add(ret, null, param.getCryptedUrl(), null, null, null);
        } else {
            final HashSet<String> dups = new HashSet<String>();
            /* TYPE_ALBUM */
            br.setFollowRedirects(true);
            /* Double-check if we got a direct-URL. */
            final GetRequest getRequest = br.createGetRequest(param.getCryptedUrl());
            final URLConnectionAdapter con = this.br.openRequestConnection(getRequest);
            try {
                if (this.looksLikeDownloadableContent(con)) {
                    add(ret, dups, param.getCryptedUrl(), getFileNameFromHeader(con), con.getLongContentLength() > 0 ? String.valueOf(con.getLongContentLength()) : null, null);
                    return ret;
                } else {
                    br.followConnection();
                }
            } finally {
                con.disconnect();
            }
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            /* Double-check for offline */
            final String albumID = new Regex(br.getURL(), TYPE_ALBUM).getMatch(0);
            final String buildID = br.getRegex("\"buildId\"\\s*:\\s*\"([^\"]+)").getMatch(0);
            if (albumID != null && buildID != null) {
                final Browser brc = br.cloneBrowser();
                brc.getPage("/_next/data/" + Encoding.urlEncode(buildID) + "/a/" + albumID + ".json");
                if (brc.getHttpConnection().getResponseCode() == 404) {
                    /* {"notFound":true} */
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            }
            final String albumjs = br.getRegex("const albumData\\s*=\\s*(\\{.*?\\})").getMatch(0);
            String fpName = new Regex(albumjs, "name\\s*:\\s*'([^\\']+)'").getMatch(0);
            if (fpName == null) {
                // bunkr.su
                fpName = br.getRegex("property\\s*=\\s*\"og:title\"\\s*content\\s*=\\s*\"(.*?)\"").getMatch(0);
            }
            if (fpName == null) {
                fpName = br.getRegex("<h1 id=\"title\"[^>]*title=\"([^\"]+)\"[^>]*>").getMatch(0);
                if (fpName == null) {
                    // bunkr.su
                    fpName = br.getRegex("<h1 id=\"title\"[^>]*>\\s*(.*?)\\s*<").getMatch(0);
                }
                if (fpName == null) {
                    // bunkr.su 2023-02-13
                    fpName = br.getRegex("<title>([^<]+) \\| Bunkr\\s*</title>").getMatch(0);
                }
            }
            final String albumDescription = br.getRegex("<span id=\"description-box\"[^>]*>([^<>\"]+)</span>").getMatch(0);
            String json = br.getRegex("<script\\s*id\\s*=\\s*\"__NEXT_DATA__\"\\s*type\\s*=\\s*\"application/json\">\\s*(\\{.*?\\})\\s*</script").getMatch(0);
            if (json != null) {
                final Map<String, Object> map = restoreFromString(json, TypeRef.MAP);
                List<Map<String, Object>> files = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(map, "props/pageProps/files");
                if (files == null) {
                    files = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(map, "props/pageProps/album/files");
                }
                if (files == null) {
                    files = new ArrayList<Map<String, Object>>();
                }
                Map<String, Object> pagePropsFile = (Map<String, Object>) JavaScriptEngineFactory.walkJson(map, "props/pageProps/file");
                if (pagePropsFile == null) {
                    pagePropsFile = (Map<String, Object>) JavaScriptEngineFactory.walkJson(map, "props/pageProps/album/file");
                }
                if (pagePropsFile != null) {
                    files.add(pagePropsFile);
                }
                if (files != null) {
                    for (final Map<String, Object> file : files) {
                        final String name = (String) file.get("name");
                        String cdn = (String) file.get("cdn");
                        if (cdn == null) {
                            cdn = (String) file.get("mediafiles");
                        }
                        final String size = StringUtils.valueOfOrNull(file.get("size"));
                        if (name != null && cdn != null) {
                            final String directurl = URLHelper.parseLocation(new URL(cdn), name);
                            add(ret, dups, directurl, name, size != null && size.matches("[0-9]+") ? size : null, size != null && !size.matches("[0-9]+") ? size : null);
                        }
                    }
                }
            }
            /* Typically cyberdrop.me/a/... */
            json = br.getRegex("dynamicEl\\s*:\\s*(\\[\\s*\\{.*?\\])").getMatch(0);
            if (json != null) {
                /* gallery mode only works for images */
                final List<Map<String, Object>> ressourcelist = (List<Map<String, Object>>) JavaScriptEngineFactory.jsonToJavaObject(json);
                for (final Map<String, Object> photo : ressourcelist) {
                    final String downloadUrl = (String) photo.get("downloadUrl");
                    final String subHtml = (String) photo.get("subHtml");
                    final String filesizeStr = new Regex(subHtml, "(\\d+(\\.\\d+)? [A-Za-z]{2,5})$").getMatch(0);
                    add(ret, dups, downloadUrl, null, null, filesizeStr);
                }
            }
            final String[] htmls = br.getRegex("<div class=\"image-container column\"[^>]*>(.*?)/p>\\s*</div>").getColumn(0);
            for (final String html : htmls) {
                String filename = new Regex(html, "target=\"_blank\" title=\"([^<>\"]+)\"").getMatch(0);
                if (filename == null) {
                    // bunkr.is
                    filename = new Regex(html, "<p\\s*class\\s*=\\s*\"name\"\\s*>\\s*(.*?)\\s*<").getMatch(0);
                }
                final String directurl = new Regex(html, "href=\"(https?://[^\"]+)\"").getMatch(0);
                if (directurl != null) {
                    final String filesizeBytes = new Regex(html, "class=\"(?:is-hidden)?\\s*file-size\"[^>]*>(\\d+) B").getMatch(0);
                    final String filesize = new Regex(html, "class=\"(?:is-hidden)?\\s*file-size\"[^>]*>([0-9\\.]+\\s+[MKG]B)").getMatch(0);
                    add(ret, dups, directurl, filename, filesizeBytes, filesize);
                }
            }
            /* 2023-02-13: bunkr.su */
            final String[] htmls2 = br.getRegex("<div class=\"grid-images_box rounded-lg[^\"]+\"(.*?)</div>\\s+</div>").getColumn(0);
            for (final String html : htmls2) {
                String filename = new Regex(html, "target=\"_blank\" title=\"([^<>\"]+)\"").getMatch(0);
                if (filename == null) {
                    // bunkr.is
                    filename = new Regex(html, "<p\\s*class\\s*=\\s*\"name\"\\s*>\\s*(.*?)\\s*<").getMatch(0);
                }
                final String directurl = new Regex(html, "href=\"(https?://[^\"]+)\"").getMatch(0);
                if (directurl != null) {
                    final String filesize = new Regex(html, "<p class=\"mt-0 dark:text-white-900\"[^>]*>([^<]+)</p>").getMatch(0);
                    add(ret, dups, directurl, filename, null, filesize);
                } else {
                    logger.warning("html Parser broken? HTML: " + html);
                }
            }
            if (ret.isEmpty()) {
                if (br.containsHTML("(?i)>\\s*0 files\\s*<") || br.containsHTML("(?i)There are no files in the album")) {
                    throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            if (fpName != null) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName).trim());
                if (param.getCryptedUrl().matches(TYPE_ALBUM)) {
                    fp.setAllowInheritance(true);
                }
                if (!StringUtils.isEmpty(albumDescription)) {
                    fp.setComment(albumDescription);
                }
                fp.addLinks(ret);
            }
        }
        return ret;
    }

    /**
     * Corrects given URL. </br>
     * Returns null if given URL is not a known stream/cdn URL. </br>
     * 2022-03-14: Especially required for bunkr.is video-URLs.
     */
    private String correctDirecturl(final String url) {
        if (url.matches(".*(bunkr\\.is|bunkr\\.ru).*")) {
            /* 2023-01-11: Cheap hotfix: Correcting such URLs to make direct-URLs out of them isn't possible anymore. */
            return null;
        }
        final Regex streamregex = new Regex(url, TYPE_STREAM);
        final Regex cdnregex = new Regex(url, TYPE_CDN);
        if (streamregex.matches()) {
            /* cdn can be empty(!) -> stream.bunkr.is -> media-files.bunkr.is */
            return "https://media-files" + StringUtils.valueOrEmpty(streamregex.getMatch(0)) + "." + this.getHost() + "/" + streamregex.getMatch(1);
        } else if (cdnregex.matches()) {
            /* cdn can be empty(!) -> cdn.bunkr.is -> media-files.bunkr.is */
            return "https://media-files" + StringUtils.valueOrEmpty(cdnregex.getMatch(0)) + "." + this.getHost() + "/" + cdnregex.getMatch(1);
        } else if (url.matches(TYPE_FS)) {
            // cyberdrop
            return url;
        } else if (url.matches(TYPE_MEDIA_FILES)) {
            // bunkr
            return url;
        } else {
            /* Unknown URL */
            return null;
        }
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }
}
