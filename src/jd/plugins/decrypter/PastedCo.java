//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.Set;

import org.jdownloader.controlling.PasswordUtils;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pasted.co", "paste3.org" }, urls = { "https?://(?:www\\.)?(?:tinypaste\\.com|tny\\.cz|pasted\\.co|controlc\\.com)/(?!tools|terms|api|contact|login|register|press)([0-9a-z]+|.*?id=[0-9a-z]+)", "https?://(?:www\\.)?paste3\\.org/\\d+/?" })
public class PastedCo extends PluginForDecrypt {
    public PastedCo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.PASTEBIN };
    }

    /** This can handle websites based on script "tinypaste". */
    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("(?i)>\\s*This page was either removed or never existed at all")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String pwRegex = "(?i)(Enter the correct password|has been password protected)";
        if (br.containsHTML(pwRegex)) {
            for (int i = 0; i <= 3; i++) {
                String id = new Regex(param.getCryptedUrl(), "/.*?id=([0-9a-z]+)$").getMatch(0);
                if (id == null) {
                    id = new Regex(param.getCryptedUrl(), "/([0-9a-z]+)/?$").getMatch(0);
                }
                final Form pwform = br.getForm(0);
                if (pwform == null || id == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                String pw = getUserInput(null, param);
                pwform.put("password_" + id, pw);
                br.submitForm(pwform);
                if (br.containsHTML(pwRegex)) {
                    continue;
                }
                break;
            }
            if (br.containsHTML("(Enter the correct password|has been password protected)")) {
                throw new DecrypterException(DecrypterException.PASSWORD);
            }
        }
        String pasteFrame = br.getRegex("frameborder=.0.\\s*id=.pasteFrame.\\s*src\\s*=\\s*.(https?://(?:www\\.)?[^/]+/[^<>\"\\']+)").getMatch(0);
        if (pasteFrame == null) {
            pasteFrame = br.getRegex("\"(https?://(?:www\\.)?[^/]+/[a-z0-9]+/fullscreen\\.php\\?hash=[a-z0-9]+\\&linenum=(false|true))\"").getMatch(0);
        }
        if (pasteFrame == null) {
            logger.warning("Decrypter broken for link: " + param);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage(pasteFrame.trim());
        String[] links = HTMLParser.getHttpLinks(br.toString(), null);
        if (links == null || links.length == 0) {
            return decryptedLinks;
        }
        final Set<String> pws = PasswordUtils.getPasswords(br.toString());
        for (String element : links) {
            final DownloadLink dl = createDownloadlink(element);
            if (pws != null && pws.size() > 0) {
                dl.setSourcePluginPasswordList(new ArrayList<String>(pws));
            }
            decryptedLinks.add(dl);
        }
        return decryptedLinks;
    }

    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}