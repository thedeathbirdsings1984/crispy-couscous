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

import java.util.HashMap;
import java.util.Map;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.config.Keep2shareConfig;

import jd.PluginWrapper;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "k2s.cc" }, urls = { "https?://(?:[a-z0-9\\-]+\\.)?(?:keep2share|k2s|k2share|keep2s|keep2)\\.cc/(?:file|preview)/(?:info/)?([a-z0-9_\\-]+)(/([^/\\?]+))?(\\?site=([^\\&]+))?" })
public class Keep2ShareCc extends K2SApi {
    public Keep2ShareCc(PluginWrapper wrapper) {
        super(wrapper);
    }

    // private final String DOMAINS_HTTP = "(https?://((www|new)\\.)?" + DOMAINS_PLAIN + ")";
    @Override
    public String[] siteSupportedNames() {
        // keep2.cc no dns
        return new String[] { "k2s.cc", "keep2share.cc", "keep2s.cc", "k2share.cc", "keep2share.com", "keep2share" };
    }

    private static Map<String, Object> HOST_MAP = new HashMap<String, Object>();

    @Override
    protected Map<String, Object> getHostMap() {
        return HOST_MAP;
    }

    @Override
    public String rewriteHost(String host) {
        if (host == null) {
            return "k2s.cc";
        }
        for (final String supportedName : siteSupportedNames()) {
            if (supportedName.equals(host)) {
                return "k2s.cc";
            }
        }
        return super.rewriteHost(host);
    }

    @Override
    public String buildExternalDownloadURL(final DownloadLink link, final PluginForHost buildForThisPlugin) {
        if (StringUtils.equals("real-debrid.com", buildForThisPlugin.getHost())) {
            return "http://k2s.cc/file/" + getFUID(link);// do not change
        } else {
            return link.getPluginPatternMatcher();
        }
    }

    @Override
    protected boolean isSpecialFUID(String fuid) {
        return fuid.contains("-") || fuid.contains("_");
    }

    @Override
    protected String getInternalAPIDomain() {
        return "k2s.cc";
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        super.resetLink(link);
    }

    @Override
    public Class<? extends Keep2shareConfig> getConfigInterface() {
        return Keep2shareConfig.class;
    }
}