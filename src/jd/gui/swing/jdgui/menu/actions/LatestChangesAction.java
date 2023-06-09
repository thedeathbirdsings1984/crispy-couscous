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
package jd.gui.swing.jdgui.menu.actions;

import java.awt.event.ActionEvent;

import org.appwork.utils.DebugMode;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.controlling.contextmenu.CustomizableAppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;

public class LatestChangesAction extends CustomizableAppAction {
    private static final long serialVersionUID = 2705114922279833817L;

    public LatestChangesAction() {
        setTooltipText(_GUI.T.action_changelog_tooltip());
        setIconKey(IconKey.ICON_CHANGELOG);
        setName(_GUI.T.action_changelog());
    }

    @Override
    public boolean isEnabled() {
        return DebugMode.TRUE_IN_IDE_ELSE_FALSE;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        CrossSystem.openURL("https://svn.jdownloader.org/projects/jd/activity");
    }
}
