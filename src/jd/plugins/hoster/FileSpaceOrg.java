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
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "file-space.org" }, urls = { "http://(www\\.)?file\\-space\\.org/files/(free|new)?get/[A-Za-z0-9\\-_]+/[^<>\"/]+\\.html" })
public class FileSpaceOrg extends PluginForHost {
    public FileSpaceOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://file-space.org/support.html";
    }

    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("/(free|new)?get/", "/get/"));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (br.containsHTML("(?i)File Link Error<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename = br.getRegex(">Имя файла:</td>\\s*<td[^>]*><span[^>]*>([^<>\"]+)\\s*</span").getMatch(0);
        final String filesize = br.getRegex(">Размер:</td>\\s*<td[^>]*><span[^>]*>([^<>\"]+)\\s*</span").getMatch(0);
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(Encoding.htmlDecode(filename.trim()));
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        final String md5 = br.getRegex(">MD5:</td>\\s*<td[^>]*><span[^>]*>([a-f0-9]{32})\\s*</span").getMatch(0);
        if (md5 != null) {
            link.setMD5Hash(md5);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        String dllink = checkDirectLink(link, "directlink");
        if (dllink == null) {
            br.getPage(link.getPluginPatternMatcher().replace("/get/", "/freeget/"));
            int wait = 30;
            final String waittime = br.getRegex("var time_load_link = (\\d+);").getMatch(0);
            if (waittime != null) {
                wait = Integer.parseInt(waittime);
            }
            sleep(wait * 1001l, link);
            final String fid = new Regex(link.getPluginPatternMatcher(), "/get/([A-Za-z0-9\\-_]+)").getMatch(0);
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.getPage("/files/getlink2/" + fid + ".html");
            dllink = br.getRegex("<a [^>]*href=\"([^\"]+)\"[^>]*>СКАЧАТЬ ФАЙЛ БЕСПЛАТНО").getMatch(0);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, true, 0);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty("directlink", dllink);
        dl.startDownload();
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
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                    return dllink;
                } else {
                    throw new IOException();
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