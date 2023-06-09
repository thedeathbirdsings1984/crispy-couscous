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

import java.io.IOException;

import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "games.reveur.de" }, urls = { "https?://(?:www\\.)?games\\.reveur\\.de/(?:en|de)/[a-z0-9\\-]+/addons/[a-z0-9\\-]+/[a-z0-9\\-]+/[a-z0-9\\-]+/\\d+/[A-Za-z0-9\\-]+\\.html" })
public class GamesReveurDe extends PluginForHost {
    public GamesReveurDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://games.reveur.de/en/impressum/";
    }

    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("/de/", "/en/"));
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getHeaders().put("Accept-Language", "de,en-us;q=0.7,en;q=0.3");
        br.getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404 || new Regex(br.getURL(), "https?://games\\.reveur\\.de/?$").matches() || br.containsHTML(">\\s*Download not available!\\s*<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<title>([^<>\"]*?) zum Download</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
        }
        String filesize = br.getRegex("Downloadgröße: ([^<>\"]*?)</td>").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("class=\"addon-info-right\">[\t\n\r ]+(\\d+ (?:kB|mB|gB))[\t\n\r ]+</div>").getMatch(0);
        }
        if (filename != null) {
            link.setName(Encoding.htmlDecode(filename.trim()));
        }
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        final String dlid = new Regex(link.getDownloadURL(), "/single/(\\d+)/").getMatch(0);
        final String dltype = br.getRegex("dlType = \\'([^<>\"]*?)\\';").getMatch(0);
        final String gameid = br.getRegex("gameId = \\'(\\d+)\\';").getMatch(0);
        final String gameshort = br.getRegex("gameShort = \\'([^<>\"]*?)\\';").getMatch(0);
        if (dltype == null || gameid == null || gameshort == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.postPage("https://games.reveur.de/includes/ajax/getDlValues.php", "");
        final String val1 = getJson("value1");
        final String val2 = getJson("value2");
        final String sign = getJson("arithmethicSign");
        if (val1 == null || val2 == null || sign == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final int value1 = Integer.parseInt(val1);
        final int value2 = Integer.parseInt(val2);
        int result;
        if (sign.equals("+")) {
            result = value1 + value2;
        } else {
            result = value1 - value2;
        }
        br.postPage("https://games.reveur.de/includes/ajax/checkUserInput.php", "params={\"val1\":" + value1 + ",\"val2\":" + value2 + ",\"result\":\"" + result + "\"}");
        final String ticket = getJson("ticket");
        if (ticket == null) {
            /* Happens sometimes ... */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 1 * 60 * 1000l);
        }
        final String dllink = "https://games.reveur.de/Download.php?intDlId=" + dlid + "&strDlType=" + dltype + "&strFileType=zip&intGameId=" + gameid + "&strGameShort=" + gameshort + "&strTicket=" + ticket;
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, false, 1);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getJson(final String parameter) {
        String result = br.getRegex("\"" + parameter + "\":(\\d+)").getMatch(0);
        if (result == null) {
            result = br.getRegex("\"" + parameter + "\":\"([^<>\"]*?)\"").getMatch(0);
        }
        return result;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}