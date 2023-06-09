package jd.gui.swing.jdgui.views.settings.sidebar;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import net.miginfocom.swing.MigLayout;

import org.appwork.utils.ColorUtils;
import org.appwork.utils.swing.renderer.RenderLabel;
import org.appwork.utils.swing.renderer.RendererCheckBox;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.images.NewTheme;
import org.jdownloader.updatev2.gui.LAFOptions;

public class ExtensionPanelListRenderer extends JPanel implements ListCellRenderer {
    private static final long serialVersionUID = 1L;
    private RenderLabel       lbl;
    private Font              orgFont;
    private Font              boldFont;
    private Color             f;
    private Color             b2;
    private Color             a;
    private RendererCheckBox  cb;
    private RenderLabel       extensionButton;
    private AbstractIcon      addIcon;
    private AbstractIcon      removeIcon;

    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        if (value instanceof CheckBoxedEntry) {
            final CheckBoxedEntry ext = (CheckBoxedEntry) value;
            // AddonConfig.getInstance(plg.getSettings(), "", true)
            cb.setVisible(true);
            extensionButton.setVisible(false);
            setText(ext.getName());
            setIcon(ext._getIcon(32));
            cb.setSelected(ext._isEnabled());
            lbl.setEnabled(cb.isSelected());
            if (isSelected) {
                lbl.setFont(boldFont);
                // lbl.setBorder(brd);
                setBackground(b2);
                // lbl.setForeground(b);
                setOpaque(true);
                // lbl.setForeground(b);
            } else {
                lbl.setFont(orgFont);
                if (index % 2 == 0) {
                    setBackground(a);
                    setOpaque(true);
                } else {
                    setOpaque(false);
                    setBackground(null);
                }
            }
        }
        return this;
    }

    private void setIcon(Icon icon) {
        lbl.setIcon(icon);
        lbl.setDisabledIcon(NewTheme.I().getDisabledIcon(icon));
    }

    private void setText(String name) {
        lbl.setText(name);
    }

    public ExtensionPanelListRenderer() {
        super(new MigLayout("ins 0 ,wrap 3", "[][grow,fill][]", "[]"));
        lbl = new RenderLabel();
        cb = new RendererCheckBox();
        addIcon = new AbstractIcon(IconKey.ICON_ADD, 20);
        removeIcon = new AbstractIcon(IconKey.ICON_REMOVE, 20);
        extensionButton = new RenderLabel();
        extensionButton.setIcon(addIcon);
        extensionButton.setVisible(false);
        add(extensionButton, "aligny top,width 20!,sg border,hidemode 3");
        add(cb, "aligny top,width 20!,sg border,hidemode 3");
        add(lbl, "");
        add(Box.createGlue(), "sg border");
        lbl.setVerticalTextPosition(JLabel.BOTTOM);
        lbl.setHorizontalTextPosition(JLabel.CENTER);
        lbl.setHorizontalAlignment(JLabel.CENTER);
        orgFont = lbl.getFont();
        boldFont = lbl.getFont().deriveFont(Font.BOLD);
        // this.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
        f = lbl.getForeground();
        b2 = ColorUtils.getAlphaInstance(f, 60);
        Color c = (LAFOptions.getInstance().getColorForPanelHeaderBackground());
        if (c != null) {
            b2 = ColorUtils.getAlphaInstance(c, 230);
        }
        a = ColorUtils.getAlphaInstance(lbl.getForeground(), 4);
        lbl.setOpaque(false);
        setOpaque(false);
        setBackground(null);
        lbl.setBackground(null);
    }
}
