package swg.gui.schematics;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.basic.BasicTableUI;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import swg.SWGAide;
import swg.crafting.SWGWeights;
import swg.crafting.Stat;
import swg.crafting.resources.SWGKnownResource;
import swg.crafting.resources.SWGResourceClass;
import swg.crafting.resources.SWGResourceSet;
import swg.crafting.schematics.SWGSchematic;
import swg.crafting.schematics.SWGSchematicsManager;
import swg.gui.SWGFrame;
import swg.gui.common.SWGDecoratedTableCellRenderer;
import swg.gui.common.SWGGui;
import swg.gui.common.SWGGuiUtils;
import swg.gui.common.SWGHelp;
import swg.gui.common.SWGJTable;
import swg.gui.common.SWGListCellRenderer;
import swg.gui.common.SWGListModel;
import swg.gui.common.SWGResourceStatRenderer;
import swg.gui.common.SWGDecoratedTableCellRenderer.DecoratedTableModel;
import swg.gui.common.SWGDecoratedTableCellRenderer.TableCellDecorations;
import swg.gui.resources.SWGInventoryWrapper;
import swg.gui.resources.SWGResController;
import swg.tools.SpringUtilities;
import swg.tools.ZNumber;

/**
 * This GUI element presents the user with features to find matching resources
 * for a selected schematic. Per schematic there are also a table for inventory,
 * and a notes field. This type scans resources that are spawning and inventory
 * and lists the best fit per experimentation line. If several experiment lines
 * are identical they are merged into one line. Resource rate and the estimated
 * rate is also computed and displayed, determined by parameters such as the
 * expertise of the assignee.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
@SuppressWarnings("serial")
final class SWGLaboratoryTab extends JPanel {

    /**
     * An action that if invoked saves the schematics notes field.
     */
    private Action actionNotesSave;

    /**
     * A combo box at which to select an assignee.
     */
    private JComboBox<SWGSchematicAssignee> assigneeCombo;

    /**
     * A menu item used in SWGAide's frame, the Edit menu.
     */
    private JMenuItem findMenu;

    /**
     * A string recently searched for, or {@code null}.
     */
    private String findTxt;

    /**
     * The URL for the schematics laboratory help page. This is the page which
     * is displayed when a sub-panel does not have its own help pages.
     */
    private final URL helpPage;

    /**
     * A table that displays inventory of the selected schematic.
     */
    private SWGJTable inventory;

    /**
     * A flag that indicates if creation of this element is complete or if it is
     * yet a stub.
     */
    private boolean isGuiFinished;

    /**
     * A text area for notes related to a schematic.
     */
    private JTextArea notesField;

    /**
     * A flag that denotes if the notes field is edited or not.
     */
    private boolean notesFieldIsEdited = false;

    /**
     * The model for the table of matching resources for experiments.
     */
    private ResourceModel resourceModel;

    /**
     * A table that displays experiment wrappers and their matching resources.
     */
    private SWGJTable resourceTable;

    /**
     * A list at which to select a schematic.
     */
    private JList<SWGSchematic> schematicList;

    /**
     * A flag that denotes if the list of schematics should display HQ, LQ, or
     * all schematics. The default value is 0 which denotes all schematics, a
     * negative values denotes LQ, and a positive value denotes HQ.
     */
    private int schematicsDisplayHqLqAll = 0;

    /**
     * The container for this GUI element, the schematic tab.
     */
    final SWGSchematicTab schemTab;

    /**
     * The most recently selected resource, or {@code null}.
     */
    private volatile SWGKnownResource selectedResource;

    /**
     * Creates an instance of this GUI element. This constructor creates just a
     * stub of this type, its content is complemented lazily on demand.
     * 
     * @param parent the container of this element
     */
    SWGLaboratoryTab(SWGSchematicTab parent) {
        this.schemTab = parent;

        helpPage = SWGAide.class.getResource(
                "docs/help_schematics_laboratory_en.html");

        parent.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                focusGained();
            }
        });
    }

    /**
     * Called when the user mouse clicks at the assignee chooser. This method,
     * if it is a right-click it displays a popup menu.
     * 
     * @param e the action that triggers the call
     */
    private void actionAssigneeMouse(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3) {
            JPopupMenu popup = new JPopupMenu();

            popup.add(schemTab.assigneeMenu());
            popup.add(makeMenuFilterAll());

            popup.show(assigneeCombo, e.getX(), e.getY());
        }
    }

    /**
     * Called when the user selects an assignee at the combo box. This method
     * invokes {@link #actionPopulateSchematics(List)}. If the argument is
     * {@code null} the invocation uses a list of all schematics.
     * 
     * @param assignee a selected assignee, or {@code null}
     */
    private void actionAssigneeSelected(SWGSchematicAssignee assignee) {
        SWGSchematicAssignee ass = assignee;
        if (ass == null) {
            ass = SWGSchematicAssignee.DEFAULT; // set default assignee
            ((SWGListModel<SWGSchematicAssignee>) assigneeCombo.getModel()).setSelectedItem(ass);
        }

        List<SWGSchematic> sl = getSchematics(ass);
        sl = SWGSchemController.schematics(sl, schematicsDisplayHqLqAll);
        Collections.sort(sl);
        actionPopulateSchematics(sl);
    }

    /**
     * Called when the user selects an element at the inventory table. This
     * method updates the notes field with possible notes for the selected
     * entry.
     */
    private void actionInventorySelected() {
        SWGSchematicWrapper w = getSelectedWrapper();

        // reset and update notes field
        notesFieldIsEdited = true; // block document the coming action call
        notesField.setText(w != null
                ? w.notes()
                : null);
        notesField.setForeground(Color.BLACK);
        notesFieldIsEdited = false;

        if (w != null) {
            SWGResourceSet spawn = SWGSchemController.spawning();
            List<SWGInventoryWrapper> inv = SWGSchemController.inventory();
            SWGExperimentWrapper.refresh(w.experiments(), spawn, inv);
            resourceModel.setElements(w.experiments());
        } else
            resourceModel.setElements(null);
    }

    /**
     * Called when the user edits the notes for a schematic.
     */
    private void actionNotesEdited() {
        if (notesFieldIsEdited) return;

        notesFieldIsEdited = true;
        notesField.setForeground(Color.RED);
    }

    /**
     * Called when the user mouse clicks the schematic notes field. If it is a
     * right click this method displays a popup dialog.
     * 
     * @param e the event that triggers the call
     */
    private void actionNotesMouse(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3) {
            JPopupMenu popup = new JPopupMenu();

            JMenuItem save = new JMenuItem("Save notes", KeyEvent.VK_S);
            save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
                    ActionEvent.CTRL_MASK));
            save.setToolTipText("Save the notes text to the current schematic");
            save.addActionListener(makeActionNotesSave());
            popup.add(save);

            popup.show(notesField, e.getX(), e.getY());
        }
    }

    /**
     * Called when an event requires the assignee combo box to be populated with
     * current assignees, this action replaces previous content.
     */
    private void actionPopulateAssignees() {
        Object prv =
                ((SWGListModel<SWGSchematicAssignee>) assigneeCombo.getModel()).getSelectedItem();

        List<SWGSchematicAssignee> as = SWGSchematicTab.assignees();
        as.add(SWGSchematicAssignee.DEFAULT);
        ((SWGListModel<SWGSchematicAssignee>) assigneeCombo.getModel()).setElements(as);

        assigneeCombo.setSelectedItem(prv); // restore, if possible
    }

    /**
     * Called when an event requires the list of schematics to be populated,
     * previous content is replaced.
     * <p>
     * This method may have side effects for other GUI elements. If there is a
     * previous schematic and it is in the specified list of schematics this
     * method does nothing further, otherwise other GUI elements are reset.
     * 
     * @param sl a list of schematics
     */
    private void actionPopulateSchematics(List<SWGSchematic> sl) {
        schematicList.setValueIsAdjusting(true); // prevent event triggering

        SWGListModel<SWGSchematic> model = ((SWGListModel<SWGSchematic>) schematicList.getModel());
        SWGSchematic prev = (SWGSchematic) model.getSelectedItem();

        schematicList.clearSelection();
        model.setElements(sl);

        if (sl.contains(prev)) schematicList.setSelectedValue(prev, true);

        schematicList.setValueIsAdjusting(false); // trigger a changed event
    }

    /**
     * Called when the user selects the option to adjust the max amount of
     * resources to display. This method displays a dialog with a slider which
     * action listener invokes {@link ResourceModel#resourceLimit(Integer)} with
     * the adjusted value.
     */
    private void actionReslimitSlider() {
        Integer[] vl = new Integer[8];
        for (int i = 0; i < vl.length; ++i)
            vl[i] = Integer.valueOf(i + 3);
        Integer v = Integer.valueOf(resourceModel.resourceLimit());
        v = (Integer) JOptionPane.showInputDialog(this,
                "Max amount of displayed resources", "Adjust max",
                JOptionPane.OK_CANCEL_OPTION, null, vl, v);
        if (v != null) resourceModel.resourceLimit(v);
    }

    /**
     * Called when the user clicks the table of matching resources and
     * experiment groups. If it is a right click this method displays a popup
     * dialog.
     * 
     * @param e the event that triggers the call
     */
    private void actionResourceTableMouse(MouseEvent e) {
        int row = resourceTable.rowAtPoint(e.getPoint());
        int cow = resourceTable.convertRowIndexToModel(row);

        if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() >= 2) {
            Object o = schematicList.getSelectedValue();
            ERPair erp = resourceModel.getElement(cow);
            SWGTestBench.update((SWGSchematic) o, erp.resource);

            return;
        }

        if (e.getButton() != MouseEvent.BUTTON3) return;

        if (row != resourceTable.getSelectedRow())
                resourceTable.getSelectionModel().setSelectionInterval(row, row);

        ERPair erp = resourceModel.getElement(cow);
        SWGKnownResource kr = erp.resource; // null-warning
        SWGResourceClass rc = erp.wrapper.rc();
        SWGResourceClass kc = kr != null
                    ? kr.rc()
                    : erp.wrapper.rc();
        SWGWeights wgs = erp.wrapper.weights();
        String nts = schematicList.getSelectedValue().getName();

        JPopupMenu popup = new JPopupMenu();

        popup.add(SWGResController.resourceDetailsMenu(kr, resourceTable));
        popup.add(SWGResController.currentSelectMenu(kr));

        popup.addSeparator();

        popup.add(SWGResController.currentFilterMenu(rc, wgs, kr));
        popup.add(SWGResController.inventoryFilterMenu(kc, wgs, kr, false));

        popup.addSeparator();

        boolean lq = wgs == SWGWeights.LQ_WEIGHTS;
        boolean ex = SWGResController.guardExist(rc, wgs, null);

        JMenuItem m = SWGResController.guardQualityMenu(rc, wgs, this, nts);
        m.setEnabled(!lq && !ex);
        popup.add(m);

        m = SWGResController.guardPlainMenu(rc, this, nts);
        m.setEnabled(lq && !ex);
        popup.add(m);

        popup.addSeparator();
        popup.add(makeMenuReslimit());

        popup.show(resourceTable, e.getX(), e.getY());
    }

    /**
     * Called when the user selects an entry at the schematics list, or at
     * reset. This method updates related GUI elements, these are the table for
     * inventory and the notes field, and the table of matching resources; if
     * the argument is {@code null} these GUI elements are cleared.
     * 
     * @param s a selected schematic, or {@code null}
     */
    private void actionSchematicSelected(SWGSchematic s) {
        selectedResource = null;
        ((SWGListModel<SWGSchematic>) schematicList.getModel()).setSelectedItem(s);

        if (s != null) {
            List<SWGSchematicWrapper> wl = SWGSchemController.wrappers(s);
            ((InventoryModel) inventory.getModel()).setElements(wl);

            // set default wrapper
            inventory.getSelectionModel().setSelectionInterval(0, 0);
            schemTab.schematicSelect(s, this);
        } else {
            ((InventoryModel) inventory.getModel()).setElements(null);
            inventory.getSelectionModel().clearSelection();
        }
    }

    /**
     * Called when the user clicks the schematic selection list. If it is a
     * right click this method displays a popup dialog.
     * 
     * @param e the event that triggers the call
     */
    private void actionSchematicsMouse(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() >= 2) {
            Object o = schematicList.getSelectedValue();
            SWGTestBench.update((SWGSchematic) o, null);
            return;
        }

        if (e.getButton() != MouseEvent.BUTTON3) return;

        JPopupMenu popup = new JPopupMenu();

        int row = schematicList.locationToIndex(e.getPoint());
        if (row != schematicList.getSelectedIndex())
            schematicList.setSelectedIndex(row);

        schemSelectAddMenu(popup);
        popup.add(makeMenuFindSchem());
        popup.addSeparator();

        ActionListener ac = new ActionListener() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void actionPerformed(ActionEvent ee) {
                String c = ee.getActionCommand();
                if (c.equals("Only HQ"))
                    schematicsDisplayHqLqAll = 1;
                else if (c.equals("Only LQ"))
                    schematicsDisplayHqLqAll = -1;
                else if (c.equals("All")) schematicsDisplayHqLqAll = 0;

                assigneeCombo.setSelectedItem(assigneeCombo.getSelectedItem());
            }
        };
        int lqAhq = schematicsDisplayHqLqAll;
        ButtonGroup bg = new ButtonGroup();
        SWGSchematicTab.schemRadioButtonMenu("Only HQ", ac, bg, popup, lqAhq);
        SWGSchematicTab.schemRadioButtonMenu("Only LQ", ac, bg, popup, lqAhq);
        SWGSchematicTab.schemRadioButtonMenu("All", ac, bg, popup, lqAhq);
        popup.addSeparator();

        popup.add(schemTab.assigneeMenu());
        popup.addSeparator();

        popup.add(makeMenuFilterAll());

        popup.show(schematicList, e.getX(), e.getY());
    }

    /**
     * Helper method which displays a find dialog. This is the starting point
     * for the user to find schematics with a given text string in their names.
     * 
     * @param txt text for the first dialog
     */
    private void findSchematic(String txt) {
        String f = (String) JOptionPane.showInputDialog(assigneeCombo,
                txt, "Find", JOptionPane.PLAIN_MESSAGE, null, null, findTxt);

        if (f == null || f.trim().isEmpty()) return;

        findTxt = f;
        List<SWGSchematic> res = SWGSchematicsManager.findSchematics(findTxt);
        if (res.size() <= 0)
            JOptionPane.showMessageDialog(assigneeCombo,
                    String.format("Find failed for \"%s\"", findTxt),
                    "No Result", JOptionPane.WARNING_MESSAGE);
        else {
            Collections.sort(res);
            List<String> sNames = new ArrayList<String>(res.size());
            for (SWGSchematic e : res)
                sNames.add(e.getName());

            String sn = (String) JOptionPane.showInputDialog(
                    assigneeCombo, "Several schematics found",
                    "Select schematic", JOptionPane.QUESTION_MESSAGE,
                    null, sNames.toArray(), sNames.get(0));

            if (sn != null) {
                SWGSchematic s = res.get(sNames.indexOf(sn));
                actionSchematicSelected(s);
            }
        }
    }

    /**
     * This method is invoked when the user selects any one of the tabs in this
     * tabbed pane, or if the collection of assignees is modified in any way. If
     * this element is yet a stub it is populated.
     */
    void focusGained() {
        if (schemTab.frame.getTabPane().getSelectedComponent() == schemTab
                && schemTab.getSelectedComponent() == this) {

            if (!isGuiFinished) make();

            actionPopulateAssignees();

            SWGHelp.push(helpPage);
            schemTab.frame.editMenuAdd(findMenu);
            schemTab.updateStatbar2();
        } else {
            schemTab.frame.editMenuRemove(findMenu);
            SWGHelp.remove(helpPage);
        }
    }

    /**
     * Helper method which returns a list of favorite schematics, an empty list,
     * or all schematics.
     * 
     * @param ass an assignee
     * @return a list of schematics
     */
    private List<SWGSchematic> getSchematics(SWGSchematicAssignee ass) {
        if (ass == SWGSchematicAssignee.DEFAULT
                && ((Boolean) SWGFrame.getPrefsKeeper().get(
                        "schemLaboratoryFilterAll",
                        Boolean.FALSE)).booleanValue())
            return SWGDraftTab.getFilteredSchematics();

        // else
        return ass.getFavorites();
    }

    /**
     * Helper method which determines which schematic wrapper that is selected
     * and returns it. If none is selected this method returns {@code null}.
     * 
     * @return a schematic wrapper, or {@code null}
     */
    private SWGSchematicWrapper getSelectedWrapper() {
        int r = inventory.getSelectedRow();
        if (r < 0) return null;

        r = inventory.convertRowIndexToModel(r);
        InventoryModel m = ((InventoryModel) inventory.getModel());
        return m.getElement(r);
    }

    /**
     * Helper method which creates the interior of this GUI element when the
     * user selects this element for the first time.
     */
    private synchronized void make() {
        if (isGuiFinished) return; // sanity

        findMenu = makeMenuFindSchem();

        this.setLayout(new BorderLayout());
        this.add(makeNorth(), BorderLayout.PAGE_START);
        this.add(makeCenter(), BorderLayout.CENTER);

        actionAssigneeSelected(null); // populate assignee combo
        isGuiFinished = true;
    }

    /**
     * Helper method that returns an action at invocation saves the current text
     * from the schematic notes field to the current schematic wrapper.
     * 
     * @return an action
     */
    private Action makeActionNotesSave() {
        if (actionNotesSave == null)
            actionNotesSave = new AbstractAction() {
                @SuppressWarnings("synthetic-access")
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (!notesFieldIsEdited) return;

                    SWGSchematicWrapper w = getSelectedWrapper();
                    if (w == null) return;

                    w.notes(notesField.getText());
                    notesFieldIsEdited = false;
                    notesField.setForeground(Color.BLACK);
                    schemTab.frame.putToStatbar("Saved schematic notes");
                }
            };
        return actionNotesSave;
    }

    /**
     * Helper method which creates and returns the center GUI element. This
     * element displays resources which are the best match for the selected
     * schematic in relation to experimentation lines and the resource rates.
     * 
     * @return a GUI component
     */
    private Component makeCenter() {
        resourceModel = new ResourceModel();
        resourceModel.resourceLimit((Integer) SWGFrame.getPrefsKeeper().get(
                "schemLaboratoryReslimit", Integer.valueOf(5)));

        resourceTable = new SWGJTable(resourceModel);
        resourceTable.setUI(new ResourceTableUI(resourceTable, resourceModel));

        TableCellRenderer dr =
                new SWGDecoratedTableCellRenderer(resourceModel) {
                    @SuppressWarnings("synthetic-access")
                    @Override
                    protected void myBackground(JTable table, Object value,
                            boolean isSelected, boolean hasFocus, int row,
                            int column, TableCellDecorations decor) {

                        boolean s = isSelected;
                        if (!s && column <= 1) {
                            ERPair val = resourceModel.getElement(row);
                            s = val.resource != null
                                    && val.resource == selectedResource;
                        }
                        super.myBackground(table, value, s, hasFocus,
                                row, column, decor);
                    }

                    @Override
                    protected void myFont(JTable table, Object value,
                            boolean isSelected, boolean hasFocus, int row,
                            int column, TableCellDecorations decor) {

                        if (decor != null && decor.value() != null)
                            setFont((Font) decor.value());
                    }
                    
                };
        resourceTable.setDefaultRenderer(String.class, dr);

        TableCellRenderer rr = new SWGResourceStatRenderer(resourceModel) {
            @Override
            protected void myFont(JTable table, Object value,
                            boolean isSelected, boolean hasFocus, int row,
                            int column, TableCellDecorations decor) {

                if (decor != null && decor.value() != null)
                    setFont((Font) decor.value());
            }
        };
        resourceTable.setDefaultRenderer(Number.class, rr);

        resourceTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resourceTable.getTableHeader().setReorderingAllowed(false);

        int w;
        w = SWGGuiUtils.fontWidth(this, "1 000", SWGGuiUtils.fontBold()) + 5;
        SWGGuiUtils.tableColumnSetWidth(resourceTable, 0, 20, 150, 150);
        SWGGuiUtils.tableColumnSetWidth(resourceTable, 2, w * 2, w * 2, w * 3);
        SWGGuiUtils.tableSetColumnWidths(resourceTable, 3, 3 + 10, w, 5);
        SWGGuiUtils.tableColumnFixWidth(resourceTable, 14, 50);

        resourceTable.getSelectionModel().addListSelectionListener(
                new ListSelectionListener() {
                    @SuppressWarnings("synthetic-access")
                    @Override
                    public void valueChanged(ListSelectionEvent e) {
                        if (!e.getValueIsAdjusting()) {
                            ERPair erp = resourceModel.getElement(
                                    resourceTable.getSelectedRow());
                            selectedResource = erp != null
                                    ? erp.resource
                                    : null;
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    resourceTable.repaint(500);
                                }
                            });
                        }
                    }
                });
        
        resourceTable.addMouseListener(new MouseAdapter() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void mouseClicked(MouseEvent e) {
                actionResourceTableMouse(e);
            }
        });

        JScrollPane jsp = new JScrollPane(resourceTable,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        return jsp;
    }

    /**
     * Helper method which creates and returns a check-box menu item for how the
     * "All-assignee" behaves. If the user toggles the value the action listener
     * saves the option in SWGAide's DAT file and with the current selection it
     * invokes {@link #actionAssigneeSelected(SWGSchematicAssignee)}.
     * 
     * @return a check-box menu option
     */
    private JCheckBoxMenuItem makeMenuFilterAll() {
        boolean bf = ((Boolean) SWGFrame.getPrefsKeeper().get(
                "schemLaboratoryFilterAll", Boolean.FALSE)).booleanValue();

        final JCheckBoxMenuItem fltd = new JCheckBoxMenuItem("Filter \"All\"");
        fltd.setToolTipText("Let \"All\" use the filters from Draft Schematics");
        fltd.setSelected(bf);
        fltd.addActionListener(new ActionListener() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void actionPerformed(ActionEvent arg0) {
                SWGFrame.getPrefsKeeper().add("schemLaboratoryFilterAll",
                        Boolean.valueOf(fltd.isSelected()));
                actionAssigneeSelected((SWGSchematicAssignee)
                        assigneeCombo.getSelectedItem());
                    }
        });
        return fltd;
    }

    /**
     * Creates and returns a menu item which if it is selected invokes
     * {@link #findSchematic(String)}.
     * 
     * @return a menu item
     */
    private JMenuItem makeMenuFindSchem() {
        final JMenuItem find = SWGSchematicTab.findSchematicMenu();
        find.addActionListener(new ActionListener() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void actionPerformed(ActionEvent ee) {
                findSchematic(find.getToolTipText());
            }
        });
        return find;
    }

    /**
     * Helper method which creates and returns a menu item for adjusting the max
     * amount of resources to display per pair of experiment/resource-class. Its
     * action listener invokes {@link #actionReslimitSlider()}.
     * 
     * @return a menu item
     */
    private JMenuItem makeMenuReslimit() {
        JMenuItem sl = new JMenuItem("Resource amount...");
        sl.setToolTipText("Adjust the max amount of resources to display");
        sl.addActionListener(new ActionListener() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void actionPerformed(ActionEvent e) {
                actionReslimitSlider();
            }
        });
        return sl;
    }

    /**
     * Helper method which creates and returns the topmost GUI element. This
     * element contains elements to select an assignee and a schematic, and for
     * the selected schematics to display what is in inventory, and its notes
     * field.
     * 
     * @return a GUI component
     */
    private Component makeNorth() {
        JPanel bp = new JPanel(new SpringLayout()) {
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                d.height = schemTab.getHeight() >> 2;
                return d;
            }
        };

        bp.add(makeNorthWest());
        bp.add(makeNorthCenter());
        bp.add(makeNorthEast());

        SpringUtilities.makeCompactGrid(bp, 1, 3, 0, 0, 3, 0);
        return bp;
    }

    /**
     * Helper method which creates and returns a GUI element for the north
     * panel. This element is the centered inventory of a selected schematic.
     * 
     * @return a GUI component
     */
    private Component makeNorthCenter() {
        Box vb = Box.createVerticalBox();
        vb.add(makeNorthCenterInv());
        vb.add(makeNorthCenterButts());
        return vb;
    }

    /**
     * Helper method which creates and returns a GUI element for the north
     * panel. This element contains some buttons for this type.
     * 
     * @return a GUI component
     */
    private Component makeNorthCenterButts() {
        Box hb = Box.createHorizontalBox();
        hb.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));

        JButton tb = new JButton("Test Bench");
        tb.setToolTipText("Open the Test Bench window");
        tb.addActionListener(new ActionListener() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void actionPerformed(ActionEvent e) {
                Object o = schematicList.getSelectedValue();
                SWGTestBench.update((SWGSchematic) o, null);
            }
        });
        hb.add(tb);

        JButton sb = new JButton("Save notes");
        sb.setToolTipText("Save the notes text to the current schematic");
        sb.addActionListener(makeActionNotesSave());
        hb.add(sb);

        return hb;
    }

    /**
     * Helper method which creates and returns a GUI element for the north
     * panel. This element is the centered inventory of a selected schematic.
     * 
     * @return a GUI component
     */
    private JScrollPane makeNorthCenterInv() {
        SWGJTable it = new SWGJTable(new InventoryModel());
        it.setToolTipText("The inventory of a selected schematic");
        it.setAutoCreateRowSorter(false);
        it.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        SWGGuiUtils.tableColumnSetWidth(it, 1, 10, 50, 70);
        it.getSelectionModel().addListSelectionListener(
                new ListSelectionListener() {
                    @SuppressWarnings("synthetic-access")
                    @Override
                    public void valueChanged(ListSelectionEvent e) {
                        if (!e.getValueIsAdjusting())
                            actionInventorySelected();
                    }
                });
        JScrollPane jsp = new JScrollPane(it) {
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                d.width = (schemTab.getWidth() >> 2);
                return d;
            }
        };
        jsp.setToolTipText("The inventory of a selected schematic");
        inventory = it;
        return jsp;
    }

    /**
     * Helper method which creates and returns a GUI element for the north
     * panel. This element is the notes field for a selected schematic.
     * 
     * @return a GUI component
     */
    private Component makeNorthEast() {
        Box bp = new Box(BoxLayout.X_AXIS) {
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                d.width = (schemTab.getWidth() >> 1);
                return d;
            }
        };
        bp.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(EtchedBorder.LOWERED), "Notes"));

        notesField = new JTextArea();
        notesField.setWrapStyleWord(true);
        notesField.setLineWrap(true);

        notesField.getInputMap().put(
                KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK),
                makeActionNotesSave());

        notesField.getDocument().addDocumentListener(new DocumentListener() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void changedUpdate(DocumentEvent e) {
                actionNotesEdited();
            }

            @SuppressWarnings("synthetic-access")
            @Override
            public void insertUpdate(DocumentEvent e) {
                actionNotesEdited();
            }

            @SuppressWarnings("synthetic-access")
            @Override
            public void removeUpdate(DocumentEvent e) {
                actionNotesEdited();
            }
        });
        notesField.addMouseListener(new MouseAdapter() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void mouseClicked(MouseEvent e) {
                actionNotesMouse(e);
            }
        });

        JScrollPane jsp = new JScrollPane(notesField);
        bp.add(jsp);
        return bp;
    }

    /**
     * Helper method which creates and returns the north-west GUI element to
     * select an assignee and a schematic. This element contains a combo-box to
     * select an assignee and a list of schematics for the selected assignee.
     * 
     * @return a GUI component
     */
    private Component makeNorthWest() {
        Box vb = new Box(BoxLayout.Y_AXIS) {
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                d.width = schemTab.getWidth() >> 2;
                return d;
            }
        };
        vb.add(makeNWAssigneeChooser());
        vb.add(Box.createVerticalStrut(2));
        vb.add(makeNWSchemChooser());
        return vb;
    }

    /**
     * Helper method which creates and returns a GUI element for the NW element.
     * This element is the combo box to select an assignee.
     * 
     * @return a GUI component
     */
    private Component makeNWAssigneeChooser() {
        assigneeCombo = new JComboBox<SWGSchematicAssignee>(new SWGListModel<SWGSchematicAssignee>());
        assigneeCombo.setToolTipText("Select assignee for favorite schematics");
        assigneeCombo.setRenderer(new SWGListCellRenderer<SWGGui>() {

            @Override
            protected String labelString(SWGGui value) {
                SWGGui ass = value;
                return ass != null
                        ? ass.getName()
                        : null;
            }
        });
        assigneeCombo.addActionListener(new ActionListener() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void actionPerformed(ActionEvent e) {
                Object o = assigneeCombo.getSelectedItem();
                actionAssigneeSelected((SWGSchematicAssignee) o);
            }
        });
        assigneeCombo.addMouseListener(new MouseAdapter() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void mouseClicked(MouseEvent e) {
                actionAssigneeMouse(e);
            }
        });
        Box vb = new Box(BoxLayout.PAGE_AXIS) {
            @Override
            public Dimension getMaximumSize() {
                Dimension d = super.getMaximumSize();
                d.height = 23;
                return d;
            }

            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                d.height = 23;
                return d;
            }
        };
        vb.add(assigneeCombo);
        return vb;
    }

    /**
     * Helper method which creates and returns a GUI element for the NW element.
     * This element is the list to select a schematics from a list of favorites.
     * 
     * @return a GUI component
     */
    private Component makeNWSchemChooser() {
        schematicList = new JList<SWGSchematic>(new SWGListModel<SWGSchematic>());
        schematicList.setToolTipText("Select a schematic");
        schematicList.setCellRenderer(new SWGListCellRenderer<SWGSchematic>() {
            @Override
            protected String labelString(SWGSchematic value) {
                SWGSchematic schem = value;
                return schem != null
                        ? schem.getName()
                        : null;
            }
        });
        schematicList.addListSelectionListener(new ListSelectionListener() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    Object o = schematicList.getSelectedValue();
                    actionSchematicSelected((SWGSchematic) o);
                }
            }
        });
        schematicList.addMouseListener(new MouseAdapter() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void mouseClicked(MouseEvent e) {
                actionSchematicsMouse(e);
            }
        });
        JScrollPane jsp = new JScrollPane(schematicList);
        return jsp;
    }

    /**
     * Selects the specified schematic at this panel. If the argument is {@code
     * null} this method does nothing, otherwise the schematic is selected. This
     * method does not consider filters applied to the assignee selector. If
     * necessary this method ready creates this panel. This method is invoked
     * from {@link SWGSchematicTab#schematicSelect(SWGSchematic, JComponent)}.
     * 
     * @param s a schematic
     */
    void schemSelect(SWGSchematic s) {
        if (s != null) {
            if (!isGuiFinished) make();
            actionSchematicSelected(s);
        }
    }

    /**
     * Helper method that adds a menu item to the specified popup menu. If a
     * schematic is selected this method adds a separator and a menu item from
     * {@link SWGSchematicTab#schematicSelectMenu(SWGSchematic, JComponent)}.
     * 
     * @param popup a popup dialog
     */
    private void schemSelectAddMenu(JPopupMenu popup) {
        SWGSchematic s = schematicList.getSelectedValue();
        popup.add(schemTab.schematicSelectMenu(s, this));
    }

    /**
     * A helper type that contains two references, an experiment wrapper and a
     * resource that pertains to the wrapper. See comments for the members.
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    private static final class ERPair {

        /**
         * The amount of {@link #resource} in inventory, {@code invent < 0}
         * means the resource is not in stock.
         * <p>
         * <b>Notice:</b> for HQ schematics the value of this member is also
         * used to denote the quality of the wrapped resource versus inventory.
         * This member can take the following values:
         * <ul>
         * <li><b><i>n </i></b>: amount in inventory</li>
         * <li><b><tt>-1</tt></b>: better than anything in inventory</li>
         * <li><b><tt>-2</tt></b>: better than <i>some </i> inventory entries or
         * this is an LQ schematic</li>
         * </ul>
         * <p>
         * Hence negative values denotes that the wrapped resource is not in
         * inventory and it is good enough to be displayed, and for the GUI its
         * value denotes whether it should be highlighted or not. LQ resources
         * are not highlighted.
         */
        final long invent;

        /**
         * A resource that pertains to the wrapper, or {@code null}. If this
         * member is {@code null} this instance denotes an experiment wrapper in
         * itself.
         */
        final SWGKnownResource resource;

        /**
         * An experiment wrapper. If {@link #resource} is not {@code null} this
         * instance denotes a resource and this members the experiment wrapper
         * the resource pertains to.
         */
        final SWGExperimentWrapper wrapper;

        /**
         * Creates an instance of this type for the specified arguments. If the
         * specified resource is {@code null} the created type denotes the
         * wrapper.
         * 
         * @param ew an experiment wrapper
         * @param kr a resource, or {@code null}
         * @param invent the amount of this resource in stock, or -1 or less,
         *        see {@link #invent}
         */
        ERPair(SWGExperimentWrapper ew, SWGKnownResource kr, long invent) {
            this.resource = kr;
            this.wrapper = ew;
            this.invent = invent;
        }
    }

    /**
     * This type is a light weight table model for schematics inventory.
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    private static final class InventoryModel extends AbstractTableModel {

        /**
         * The current list of schematics.
         */
        private List<SWGSchematicWrapper> wraps;

        /**
         * Creates an instance of this table model.
         */
        InventoryModel() {
            super();
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0) return String.class;
            if (columnIndex == 1) return Integer.class;
            return null;
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int column) {
            if (column == 0) return "Serial";
            if (column == 1) return "Count";
            return null;
        }

        /**
         * Returns the schematic wrapper for the specified row. The argument
         * must be converted to the model.
         * 
         * @param row a line number
         * @return a schematic wrapper
         */
        SWGSchematicWrapper getElement(int row) {
            return wraps.get(row);
        }

        @Override
        public int getRowCount() {
            return wraps != null
                    ? wraps.size()
                    : 0;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (wraps == null) return null;

            SWGSchematicWrapper sw = wraps.get(rowIndex);
            if (columnIndex == 0) return sw.serial();
            if (columnIndex == 1) return Integer.valueOf(sw.stock());
            return "ERROR";
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            // default wrapper at row 0, stock at column 1 is editable
            return (rowIndex > 0 || columnIndex > 0);
        }

        /**
         * Sets the content for this model to the specified list. The argument
         * should not be used further by the caller.
         * 
         * @param wrappers a list of schematic wrappers
         */
        void setElements(List<SWGSchematicWrapper> wrappers) {
            wraps = wrappers;
            super.fireTableDataChanged();
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            SWGSchematicWrapper sw = wraps.get(rowIndex);
            if (columnIndex == 0) {
                if (sw.isDefault())
                    SWGAide.printError("SWGLaboratoryTab:setValueAt:inventory",
                            new IllegalStateException("default s-wrapper"));

                sw.serial((String) aValue);
            } else if (columnIndex == 1)
                sw.stock(((Integer) aValue).intValue());
        }
    }

    /**
     * This type is a light weight table model for matching resources.
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    private final class ResourceModel extends AbstractTableModel
            implements DecoratedTableModel {

        /**
         * Column titles.
         */
        private final String[] cols =
                { "Name", "Class", "Stock", "ER", "CR", "CD", "DR", "FL", "HR",
                        "MA", "PE", "OQ", "SR", "UT", "Rate" };

        /**
         * A convenience constant array of stats in game order.
         */
        private final Stat[] gOrder = Stat.gameOrder();

        /**
         * A list of elements for this model. In particular, the elements are
         * pair that denotes an experiment wrapper or a known resource. In the
         * latter case a reference denotes which wrapper the resource pertains
         * to and it is used to compute the rate of the resource. If the element
         * denotes an experiment wrapper the reference for a resource is always
         * {@code null}.
         * <p>
         * For each wrapper this list contains as many resource elements as the
         * wrapper's resource set or {@link #resLimit}, whichever is the lowest
         * value.
         */
        private List<ERPair> pairs;

        /**
         * A constant for the weight of recycled resources.
         */
        private final Double RC_WEIGHT = Double.valueOf(200.0);

        /**
         * The limit for the number of resources to display per experiment
         * wrapper, the wrapper excluded.
         */
        private int resLimit = 5;

        /**
         * A list of experiment wrappers for the selected schematic.
         */
        private List<SWGExperimentWrapper> wraps;

        /**
         * Creates an instance of this type.
         */
        ResourceModel() {
            super();
        }
        
        @SuppressWarnings("synthetic-access")
        @Override
        public TableCellDecorations getCellDecor(int row, int column,
                Object value) {

            ERPair val = pairs.get(row);
            SWGExperimentWrapper ew = val.wrapper;
            Font bold = SWGGuiUtils.fontBold();
            Font plain = SWGGuiUtils.fontPlain();

            if (val.resource == null) {
                Color bg = resourceTable.getTableHeader().getBackground();
                if (column > 0)
                    return new TableCellDecorations(bg,
                            null, null, null, (Object[]) null);

                return new TableCellDecorations(bg, null, null, bold);
            }

            String toolTip, n;
            SWGKnownResource kr = val.resource;
            if (val.invent >= 0) {
                n = SWGResController.inventoryNotes(
                        kr, SWGFrame.getSelectedGalaxy());
                if (!n.isEmpty()) n = "    Notes: " + n;
                toolTip = String.format("Units owned: %s%s",
                        ZNumber.asText(val.invent, true, true), n);
            } else
                toolTip = SWGResController.dateString(kr.age()) + " old";

            if (column <= 2) {
                Color bg = null;
                if (val.invent >= 0)
                    bg = SWGGuiUtils.statColors[0];
                else if (val.invent == -1)
                    bg = SWGGuiUtils.statColors[4];

                return new TableCellDecorations(bg, null, toolTip, plain);
            }

            SWGWeights w = ew.weights();
            boolean hq = w != SWGWeights.LQ_WEIGHTS;
            if (column < 14) {
                Stat s = gOrder[column - 3];
                Font font = hq && w.value(s) > 0
                        ? bold
                        : plain;
                int v = kr.stats().value(s);
                int c = hq
                        ? ew.rc().max(s)
                        : kr.rc().isSpaceOrRecycled()
                                ? ew.rc().max(s)
                                : kr.rc().max(s);

                return new TableCellDecorations(
                        SWGResourceStatRenderer.getStatBackGround(v, c),
                        SWGResourceStatRenderer.getStatForeground(v, c),
                        toolTip, font);
            }

            String tt = hq
                    ? "HQ -- rate by the displayed experiment line"
                    : "LQ -- rate by the resource's own class";

            // else rate
            double rate = ((Double) value).doubleValue();
            return new TableCellDecorations(
                    SWGResourceStatRenderer.getStatBackGround(rate),
                    SWGResourceStatRenderer.getStatForeground(rate),
                    tt, plain);
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex <= 1) return String.class;
            return Number.class;
        }

        @Override
        public int getColumnCount() {
            return cols.length;
        }

        @Override
        public String getColumnName(int column) {
            return cols[column];
        }

        /**
         * Returns the element of this model specified by the argument. This
         * element considers resLimit <i>and&nbsp;</i> the size of the set of
         * resources per experiment wrapper and returns an element that matches
         * the argument. If the argument is less than zero or if this model is
         * empty this method returns {@code null}.
         * 
         * @param row a row index
         * @return an element for the index, or {@code null}
         * @throws IndexOutOfBoundsException if the index is out of bounds
         */
        ERPair getElement(int row) {
            if (pairs == null || row < 0) return null;
            return pairs.get(row);
        }

        @Override
        public int getRowCount() {
            return pairs != null
                    ? pairs.size()
                    : 0;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            ERPair val = pairs.get(rowIndex);
            SWGExperimentWrapper ew = val.wrapper;

            if (val.resource == null) {
                if (columnIndex > 0)
                    return null;

                return String.format(
                        "  %s     %s", ew.getDescription(), ew.getName());
            }

            // { "Name", "Class", "Stock", "ER", "CR", "CD", "DR", "FL", "HR",
            // "MA", "PE", "OQ", "SR", "UT", "Rate" };

            SWGKnownResource kr = val.resource;
            if (columnIndex == 0)
                return kr.getName();
            if (columnIndex == 1)
                return kr.rc().rcName();
            if (columnIndex == 2)
                return val.invent > 0
                        ? Long.valueOf(val.invent)
                        : null;
            if (columnIndex < 14) {
                Stat s = gOrder[columnIndex - 3];
                return Integer.valueOf(kr.stats().value(s));
            }

            SWGResourceClass rc = kr.rc();
            SWGWeights ww = ew.weights();

            if (ww == SWGWeights.LQ_WEIGHTS && rc.isSpaceOrRecycled())
                return RC_WEIGHT;

            rc = ww == SWGWeights.LQ_WEIGHTS
                            ? rc
                            : ew.rc();
            double w = ww.rate(kr, rc, ww != SWGWeights.LQ_WEIGHTS);
            return Double.valueOf(w);
        }

        /**
         * Returns the limit for the number resources to display per experiment
         * wrapper, the wrapper excluded. Minimum value is 3 and default is 5.
         * 
         * @return the limit for then number of resources to display
         */
        int resourceLimit() {
            return resLimit;
        }

        /**
         * Sets the limit for the number resources to display per experiment
         * wrapper, the wrapper excluded. This method also updates the content
         * of this model and fires an updated-model event, finally the value is
         * added to SWGAide's preference keeper.
         * <p>
         * Suggested uses are default, user specified value, or dynamically set
         * when the frame's size is changed and the number of rows are
         * determined from how many rows the table can display.
         * 
         * @param limit the limit for the number of resources to display
         */
        void resourceLimit(Integer limit) {
            int lt = limit.intValue();
            if (lt == this.resLimit) return;
            this.resLimit = lt < 3 || lt > 10
                    ? 3
                    : lt;
            setElements(wraps);
            SWGFrame.getPrefsKeeper().add("schemLaboratoryReslimit", limit);
        }

        /**
         * Sets the elements for this model. If the argument is {@code null} the
         * content for this model is cleared. The wrappers in the list must have
         * been initiated.
         * 
         * @param elements a list of experiment wrappers, or {@code null}
         * @throws NullPointerException if the specified list of experiment
         *         wrappers is not initialized
         */
        void setElements(List<SWGExperimentWrapper> elements) {
            wraps = elements;
            if (elements == null)
                pairs = null;
            else {
                List<ERPair> tmp =
                        new ArrayList<ERPair>(wraps.size() * (resLimit + 1));

                for (SWGExperimentWrapper ew : wraps) {
                    ERPair erp = new ERPair(ew, null, -2); // wrap itself
                    tmp.add(erp);

                    SWGResourceSet rSet = ew.resources();
                    if (rSet == null) continue;

                    // see comment on ERPair#invent for the reasons behind this
                    boolean invFound = ew.weights() == SWGWeights.LQ_WEIGHTS
                            ? true // avoid highlighting for LQ-stuff
                            : false;

                    for (int i = 0; i < resLimit && i < rSet.size(); ++i) {
                        SWGKnownResource kr = rSet.get(i);
                        long invent = SWGResController.inventoryAmount(
                                        kr, SWGFrame.getSelectedGalaxy());
                        erp = new ERPair(ew, kr, invent >= 0
                                ? invent
                                : invFound
                                        ? -2 // not in inventory but not first
                                        : -1); // not in inventory and superior
                        invFound |= invent >= 0;
                        tmp.add(erp);
                    }
                }

                pairs = tmp;
            }
            super.fireTableDataChanged();
        }
    }

    /**
     * This type overrides paint methods in the default UI for the resource
     * table. This allows experiment header rows to span several columns.
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    private static final class ResourceTableUI extends BasicTableUI {

        /**
         * The model for this instance.
         */
        private final ResourceModel rModel;

        /**
         * The table for this instance.
         */
        private final JTable rTable;

        /**
         * Creates an instance of this type.
         * 
         * @param resTable the table for this type
         * @param resModel the model for this type
         */
        ResourceTableUI(JTable resTable, ResourceModel resModel) {
            super();
            this.rTable = resTable;
            this.rModel = resModel;
        }

        /**
         * Helper method for headers rows only, that span several columns;
         * compare with {@link JTable#getCellRect(int, int, boolean)}. This
         * method is only invoked by {@code ResourceTableUI#paintCells(...)} and
         * it always begins at column zero because that is the main column for a
         * header line, the only column with content.
         * 
         * @param row a row index
         * @param colMax max column index to display
         * @return a rectangle for the area to paint
         */
        private Rectangle getCellRect(int row, int colMax) {
            TableColumnModel cm = rTable.getColumnModel();
            Rectangle r = rTable.getCellRect(row, 0, false);

            for (int i = 1; i <= colMax; ++i)
                r.width += cm.getColumn(i).getWidth();

            r.x++;
            r.y++;
            r.width--;
            r.height--;
            return r;
        }

        @Override
        public void paint(Graphics g, JComponent c) {
            // for details, see super.paint(g, c)

            Rectangle clip = g.getClipBounds();
            Rectangle bounds = rTable.getBounds();
            bounds.x = bounds.y = 0;

            if (rModel.getRowCount() <= 0
                    || rModel.getColumnCount() <= 0
                    || !bounds.intersects(clip)) { return; }

            Point upperLeft = clip.getLocation();
            Point lowerRight = new Point(clip.x + clip.width - 1,
                    clip.y + clip.height);

            int rMin = rTable.rowAtPoint(upperLeft);
            int rMax = rTable.rowAtPoint(lowerRight);
            if (rMin == -1) rMin = 0;
            if (rMax == -1) rMax = rTable.getRowCount() - 1;

            int cMin = rTable.columnAtPoint(upperLeft);
            int cMax = rTable.columnAtPoint(lowerRight);
            if (cMin == -1) cMin = 0;
            if (cMax == -1) cMax = rTable.getColumnCount() - 1;

            // Paint the grid.
            paintGrid(g, rMin, rMax, cMin, cMax);

            // Paint the cells.
            paintCells(g, rMin, rMax, cMin, cMax);
        }

        /**
         * Paints the specified rectangle, a cell.
         * 
         * @param g a graphics object
         * @param cellRect the rectangle area to draw
         * @param row current row
         * @param col current column
         */
        private void paintCell(Graphics g, Rectangle cellRect, int row, int col) {

            // This method overrides the super type because it is invoked in
            // this sub-type and its invocations must stay in the local scope

            TableCellRenderer renderer = rTable.getCellRenderer(row, col);
            Component component = rTable.prepareRenderer(renderer, row, col);
            rendererPane.paintComponent(g, component, rTable, cellRect.x,
                    cellRect.y, cellRect.width, cellRect.height, true);
        }

        /**
         * Paints the cells in the specified area.
         * 
         * @param g a graphics object
         * @param rMin the first row to paint
         * @param rMax the final row to paint
         * @param cMn the first column to paint
         * @param cMx the first column to paint
         */
        private void paintCells(Graphics g, int rMin, int rMax, int cMn, int cMx) {

            // It is here the job is done that allows for cells to span over
            // several columns.

            TableColumnModel cm = rTable.getColumnModel();
            int columnMargin = cm.getColumnMargin();

            // to avoid partly unpainted header rows we must each call ensure we
            // paint it all, at least once; otherwise if col > 0 it won't update
            boolean headerNotPainted = true;

            int columnWidth;
            for (int row = rMin; row <= rMax; row++) {
                Rectangle cellRect;
                if (rModel.getElement(row).resource == null) {
                    // this is a header row
                    if (headerNotPainted) {
                        cellRect = getCellRect(row, cMx);
                        paintCell(g, cellRect, row, 0);
                        headerNotPainted = false;
                    }
                    continue;
                } // else
                headerNotPainted = true; // reset for following rows

                // else ... a resource row
                cellRect = rTable.getCellRect(row, cMn, false);
                for (int column = cMn; column <= cMx; column++) {
                    TableColumn aColumn = cm.getColumn(column);
                    columnWidth = aColumn.getWidth();
                    cellRect.width = columnWidth - columnMargin;
                    paintCell(g, cellRect, row, column);
                    cellRect.x += columnWidth;
                }
            }
        }

        /**
         * See {@code BasicTableUI#paintGrid(g, int, int, int, int)}.
         * 
         * @param g a graphics object
         * @param rMin the first row to paint
         * @param rMax the final row to paint
         * @param cMn the first column to paint
         * @param cMx the final column to paint
         */
        private void paintGrid(Graphics g, int rMin, int rMax, int cMn, int cMx) {
            Color gc = rTable.getGridColor();
            g.setColor(gc);

            Rectangle minCell = rTable.getCellRect(rMin, cMn, true);
            Rectangle maxCell = rTable.getCellRect(rMax, cMx, true);
            Rectangle dmgArea = minCell.union(maxCell);

            int tableHeight = dmgArea.y + dmgArea.height;
            int x = dmgArea.x;
            int tableWidth = dmgArea.x + dmgArea.width;
            int y = dmgArea.y;

            // vertical lines
            TableColumnModel cm = rTable.getColumnModel();
            for (int column = cMn; column <= cMx; column++) {
                int w = cm.getColumn(column).getWidth();
                x += w;
                g.drawLine(x - 1, 0, x - 1, tableHeight - 1);
            }

            // horizontal lines
            for (int row = rMin; row <= rMax; row++) {
                int rh = rTable.getRowHeight(row);
                g.drawLine(dmgArea.x, y + rh - 1, tableWidth - 1, y + rh - 1);
                if (rModel.getElement(row).resource == null) {
                    // override vertical grid lines
                    g.setColor(rTable.getTableHeader().getBackground());
                    g.fillRect(dmgArea.x, y, tableWidth - 1, rh - 1);
                    g.setColor(Color.WHITE);
                    g.drawLine(dmgArea.x, y, dmgArea.x + tableWidth - 2, y);
                    g.drawLine(dmgArea.x, y, dmgArea.x, rh);
                    g.setColor(gc);
                }
                y += rh;
            }
        }
    }
}
