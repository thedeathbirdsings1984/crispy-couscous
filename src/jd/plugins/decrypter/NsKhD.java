//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "1kh.de" }, urls = { "https?://(?:www\\.)?1kh\\.de/(?:f/[0-9/]+|[0-9]+)" })
public class NsKhD extends PluginForDecrypt {
    final static private Pattern patternSupported_File = Pattern.compile("https?://[\\w\\.]*?1kh\\.de/[0-9]+", Pattern.CASE_INSENSITIVE);

    // final static private Pattern patternSupported_Folder =
    // Pattern.compile("http://[\\w\\.]*?1kh\\.de/f/[0-9/]+",
    // Pattern.CASE_INSENSITIVE);
    public NsKhD(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        if (Regex.matches(param.getCryptedUrl(), patternSupported_File)) {
            /* Einzelne Datei */
            final String[] links = br.getRegex("<iframe name=\"pagetext\" height=\".*?\" frameborder=\"no\" width=\"100%\" src=\"(.*?)\"></iframe>").getColumn(0);
            if (links == null || links.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            progress.setRange(links.length);
            for (String element : links) {
                final Browser brc = br.cloneBrowser();
                brc.getPage(Encoding.htmlDecode(element));
                final String link = brc.getRegex("<iframe name=\"pagetext\" height=\".*?\" frameborder=\"no\" width=\"100%\" src=\"(.*?)\"></iframe>").getMatch(0);
                if (link == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(link)));
                progress.increase(1);
            }
        } else {
            Form[] forms = br.getForms();
            /* ganzer Ordner */
            boolean valid = true;
            if (forms != null && forms.length > 0 && forms[0].hasInputFieldByName("Password")) {
                valid = false;
                /* Ordner ist Passwort geschützt */
                for (int retrycounter = 1; retrycounter <= 5; retrycounter++) {
                    String password = getUserInput(null, param);
                    forms[0].put("Password", password);
                    br.submitForm(forms[0]);
                    if (!br.containsHTML("Das eingegebene Passwort ist falsch!")) {
                        valid = true;
                        break;
                    }
                }
            }
            if (valid == false) {
                throw new DecrypterException(DecrypterException.PASSWORD);
            }
            final String[] links = br.getRegex("<div class=\"Block3\".*?<a id=\"DownloadLink_(\\d+)\"").getColumn(0);
            if (links == null || links.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            progress.setRange(links.length);
            for (String element : links) {
                decryptedLinks.add(createDownloadlink("https://1kh.de/" + element));
                progress.increase(1);
            }
        }
        return decryptedLinks;
    }

    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}