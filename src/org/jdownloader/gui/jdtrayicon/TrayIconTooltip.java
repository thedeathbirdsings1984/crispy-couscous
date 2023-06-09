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
package org.jdownloader.gui.jdtrayicon;

import javax.swing.JLabel;
import javax.swing.JPanel;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.gui.swing.components.JWindowTooltip;
import jd.gui.swing.jdgui.components.JDProgressBar;
import jd.nutils.Formatter;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import net.miginfocom.swing.MigLayout;

import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.controlling.AggregatedNumbers;
import org.jdownloader.gui.jdtrayicon.translate._TRAY;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;

public class TrayIconTooltip extends JWindowTooltip {
    private static final long serialVersionUID = -400023413449818691L;
    private JLabel            lblSpeed;
    private JLabel            lblDlRunning;
    private JDProgressBar     prgTotal;
    private JLabel            lblETA;
    private JLabel            lblProgress;

    public TrayIconTooltip() {
        setName("JDownloader");
    }

    protected void addContent(JPanel panel) {
        panel.setLayout(new MigLayout("wrap 2", "[fill, grow][fill, grow]"));
        panel.add(new JLabel(_TRAY.T.plugins_optional_trayIcon_downloads()), "spanx 2");
        panel.add(new JLabel(_TRAY.T.plugins_optional_trayIcon_dl_running()), "gapleft 10");
        panel.add(lblDlRunning = new JLabel("0"));
        panel.add(new JLabel(_TRAY.T.plugins_optional_trayIcon_speed()));
        panel.add(lblSpeed = new JLabel("0 B/s"));
        panel.add(new JLabel(_TRAY.T.plugins_optional_trayIcon_progress()));
        panel.add(lblProgress = new JLabel("--.-- Mib / --.-- Mib"));
        panel.add(prgTotal = new JDProgressBar(), "spanx 2");
        panel.add(new JLabel(_TRAY.T.plugins_optional_trayIcon_eta()));
        panel.add(lblETA = new JLabel("00d:00h:00m:00s"));
    }

    @Override
    protected void updateContent() {
        final Thread thread = Thread.currentThread();
        final SelectionInfo<FilePackage, DownloadLink> selection = DownloadsTableModel.getInstance().getTable().getSelectionInfo(false, false);
        if (selection != null) {
            final AggregatedNumbers dla = new AggregatedNumbers(selection);
            new EDTRunner() {
                @Override
                protected void runInEDT() {
                    if (isVisible()) {
                        long totalDl = dla.getTotalBytes();
                        long curDl = dla.getLoadedBytes();
                        lblDlRunning.setText(String.valueOf(DownloadWatchDog.getInstance().getRunningDownloadLinks().size()));
                        lblSpeed.setText(Formatter.formatReadable(DownloadWatchDog.getInstance().getDownloadSpeedManager().getSpeed()) + "/s");
                        lblProgress.setText(Formatter.formatFilesize(curDl, 0) + " / " + Formatter.formatFilesize(totalDl, 0));
                        prgTotal.setMaximum(totalDl);
                        prgTotal.setValue(curDl);
                        lblETA.setText(dla.getEtaString());
                    } else {
                        updater.compareAndSet(thread, null);
                    }
                }
            }.waitForEDT();
        }
    }
}