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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class ArdaudiothekDe extends PluginForDecrypt {
    public ArdaudiothekDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "ardaudiothek.de" });
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

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(sendung/[\\w\\-]+/\\d+/?|episode/[\\w\\-]+/[\\w\\-]+/[\\w\\-]+/\\d+/?)");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        String episodeTargetID = null;
        final boolean doSpecialHandlingToFindSingleEpisodePosition = true;
        br.setFollowRedirects(true);
        List<Map<String, Object>> episodes = null;
        int infiniteLoopPreventionCounter = -1;
        FilePackage fp = null;
        String urlToAccess = param.getCryptedUrl();
        Map<String, Object> publicationService = null;
        do {
            infiniteLoopPreventionCounter++;
            br.getPage(urlToAccess);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            urlToAccess = null;
            final String json = br.getRegex("type=\"application/json\">([^<]+)<").getMatch(0);
            final Map<String, Object> entries = restoreFromString(json, TypeRef.MAP);
            final Map<String, Object> podcast = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "props/pageProps/initialData/data/result");
            final Map<String, Object> podcastEpisode = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "props/pageProps/initialData/data/item");
            if (podcast == null && podcastEpisode == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (podcast != null) {
                publicationService = (Map<String, Object>) podcast.get("publicationService");
                final String podcastTitle = podcast.get("title").toString();
                final String podcastDescription = (String) podcast.get("description");
                fp = FilePackage.getInstance();
                fp.setName(podcastTitle);
                fp.setComment(podcastDescription);
                fp.addLinks(ret);
                final Map<String, Object> items = (Map<String, Object>) podcast.get("items");
                episodes = (List<Map<String, Object>>) items.get("nodes");
                break;
            } else {
                /* Single episode */
                publicationService = (Map<String, Object>) podcastEpisode.get("publicationService");
                episodeTargetID = podcastEpisode.get("id").toString();
                if (doSpecialHandlingToFindSingleEpisodePosition) {
                    /*
                     * Access main podcast URL but then later only pick this episode so we get to know the position of that single added
                     * episode.
                     */
                    final Map<String, Object> programSet = (Map<String, Object>) podcastEpisode.get("programSet");
                    urlToAccess = programSet.get("path").toString();
                    logger.info("Accessing main podcast URL: " + urlToAccess);
                    continue;
                } else {
                    episodes = new ArrayList<Map<String, Object>>();
                    episodes.add(podcastEpisode);
                    break;
                }
            }
        } while (infiniteLoopPreventionCounter <= 1 && urlToAccess != null);
        // String tvRadioStationTitle = null;
        // if (publicationService != null) {
        // tvRadioStationTitle = publicationService.get("title").toString();
        // }
        final int padLength = StringUtils.getPadLength(episodes.size());
        int position = 1;
        for (final Map<String, Object> episode : episodes) {
            final String episodeID = episode.get("id").toString();
            final String episodeSummary = (String) episode.get("summary");
            /* Is available if we open the single URL to an episode. */
            final String episodeFullDescription = (String) episode.get("description");
            final List<Map<String, Object>> audios = (List<Map<String, Object>>) episode.get("audios");
            /* Find direct-url: We prefer original downloadurl and only use streaming URL as fallback. */
            String urlStreaming = null;
            String urlDownload = null;
            for (final Map<String, Object> audio : audios) {
                if ((Boolean) audio.get("allowDownload")) {
                    urlDownload = audio.get("downloadUrl").toString();
                } else {
                    urlStreaming = audio.get("url").toString();
                }
            }
            if (StringUtils.isEmpty(urlStreaming) && StringUtils.isEmpty(urlDownload)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final DownloadLink link = this.createDownloadlink(urlDownload != null ? urlDownload : urlStreaming);
            if (episodes.size() == 1) {
                link.setFinalFileName(episode.get("title") + ".mp3");
            } else {
                link.setFinalFileName(StringUtils.formatByPadLength(padLength, position) + " - " + episode.get("title") + ".mp3");
            }
            link.setAvailable(true);
            if (fp != null) {
                link._setFilePackage(fp);
            }
            if (!StringUtils.isEmpty(episodeFullDescription)) {
                link.setComment(episodeFullDescription);
            } else if (!StringUtils.isEmpty(episodeSummary)) {
                link.setComment(episodeSummary);
            }
            /* Estimate filesize based on bitrate of 160kb/s */
            final Number durationSeconds = (Number) episode.get("duration");
            if (durationSeconds != null) {
                link.setDownloadSize((durationSeconds.longValue() * 160 * 1024) / 8);
            }
            final String path = (String) episode.get("path");
            if (!StringUtils.isEmpty(path)) {
                link.setContentUrl(br.getURL(path).toString());
            }
            if (StringUtils.equals(episodeID, episodeTargetID)) {
                /* User wants to have one specific episode only */
                ret.clear();
                ret.add(link);
                break;
            } else {
                ret.add(link);
            }
            position++;
        }
        return ret;
    }
}
