package jd.gui.swing.jdgui.views.settings.panels.packagizer;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import jd.gui.swing.jdgui.views.settings.components.SettingsComponent;
import jd.gui.swing.jdgui.views.settings.components.StateUpdateListener;
import net.miginfocom.swing.MigLayout;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtButton;
import org.appwork.swing.exttable.utils.MinimumSelectionObserver;
import org.jdownloader.controlling.packagizer.PackagizerRule;
import org.jdownloader.gui.translate._GUI;

public class PackagizerFilter extends JPanel implements SettingsComponent {
    private static final long     serialVersionUID = 6070464296168772795L;
    private MigPanel              tb;
    private PackagizerFilterTable table;
    private JButton               btAdd;
    private JButton               btRemove;
    private ExtButton             btImport;
    private ExtButton             btExport;

    public PackagizerFilter() {
        super(new MigLayout("ins 0,wrap 1", "[grow,fill]", "[grow,fill][]"));
        tb = new MigPanel("ins 0", "[][][grow,fill][][]", "[]");
        tb.setOpaque(false);
        table = new PackagizerFilterTable(this);
        tb.add(btAdd = new JButton(new NewAction(table)), "height 26!,sg 1");
        RemoveAction ra;
        tb.add(btRemove = new JButton(ra = new RemoveAction(table)), "height 26!,sg 1");
        tb.add(Box.createHorizontalGlue());
        tb.add(btImport = new ExtButton(new ImportAction(this)), "height 26!,sg 2");
        tb.add(btExport = new ExtButton(new ExportAction(this, null)), "height 26!,sg 2");
        table.getModel().addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                btExport.setEnabled(table.getRowCount() > 0);
            }
        });
        setOpaque(false);
        table.getSelectionModel().addListSelectionListener(new MinimumSelectionObserver(table, ra, 1) {
            public void valueChanged(final ListSelectionEvent e) {
                boolean en = true;
                int count = 0;
                for (PackagizerRule rule : PackagizerFilter.this.table.getModel().getSelectedObjects()) {
                    en &= !rule.isStaticRule();
                    count++;
                }
                if (!en) {
                    btRemove.setToolTipText(_GUI.T.PackagizerFilter_valueChanged_disable_static());
                    action.setEnabled(false);
                    return;
                } else {
                    btRemove.setToolTipText(null);
                }
                this.action.setEnabled(count >= this.minSelections);
            }
        });
        add(new JScrollPane(table));
        add(tb);
    }

    public PackagizerFilterTable getTable() {
        return table;
    }

    public String getConstraints() {
        return "height 60:n:n,pushy,growy";
    }

    public boolean isMultiline() {
        return false;
    }

    public void addStateUpdateListener(StateUpdateListener listener) {
        throw new IllegalStateException("Not implemented");
    }
}
