package com.fourpart.logfileviewer;

import com.fourpart.logfileviewer.filter.NamedFilter;
import com.fourpart.logfileviewer.filter.NamedFilterRegistry;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.TreeSet;

public class OldNamedFilterRegistryEditorDialog extends JDialog {

    private NamedFilterRegistry namedFilterRegistry;

    private JButton editButton;
    private JButton deleteButton;

    private DefaultListModel<String> filterListModel;

    private JList<String> filterList;

    public OldNamedFilterRegistryEditorDialog(Frame owner, NamedFilterRegistry namedFilterRegistry) {

        super(owner, true);

        this.namedFilterRegistry = namedFilterRegistry;

        setTitle("Named Filter Editor");

        setSize(getInitialSize());
        setLocationRelativeTo(owner);

        GridBagPanel mainPanel = new GridBagPanel();

        mainPanel.setBorder(new EmptyBorder(new Insets(10, 10, 10, 10)));

        filterListModel = new DefaultListModel<>();

        updateFilterNames();

        filterList = new JList<>(filterListModel);
        filterList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        filterList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                int[] selected = filterList.getSelectedIndices();
                editButton.setEnabled(selected.length == 1);
                deleteButton.setEnabled(selected.length > 0);
            }
        });

        filterList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {

                if (evt.getClickCount() == 2) {

                    int index = filterList.locationToIndex(evt.getPoint());

                    if (index >= 0) {
                        editNamedFilter(filterListModel.get(index));
                    }
                }
            }
        });

        JButton addButton = new JButton("Add...");

        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addNamedFilter();
            }
        });

        editButton = new JButton("Edit...");

        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editNamedFilter(filterList.getSelectedValue());
            }
        });

        editButton.setEnabled(false);

        deleteButton = new JButton("Delete...");

        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteNamedFilter();
            }
        });

        deleteButton.setEnabled(false);

        JButton doneButton = new JButton("Done");

        doneButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        mainPanel.addComponent(new JScrollPane(filterList), 0, 0, 1, 4, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 6, 6));
        mainPanel.addComponent(addButton, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0));
        mainPanel.addComponent(editButton, 1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(6, 0, 0, 0));
        mainPanel.addComponent(deleteButton, 2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(6, 0, 0, 0));
        mainPanel.addComponent(new JPanel(), 3, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0));
        mainPanel.addComponent(doneButton, 4, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0));

        getContentPane().add(BorderLayout.CENTER, mainPanel);
    }

    private Dimension getInitialSize() {
        return new Dimension(540, 470);
    }

    private void updateFilterNames() {

        filterListModel.clear();

        for (String name : new TreeSet<>(namedFilterRegistry.getNames())) {
            filterListModel.addElement(name);
        }
    }

    private void addNamedFilter() {

        OldNamedFilterEditorDialog dialog = new OldNamedFilterEditorDialog((Frame)getOwner(), namedFilterRegistry, null);
        dialog.setTitle("Add Named Filter");
        dialog.setVisible(true);

        if (dialog.isCanceled()) {
            return;
        }

        NamedFilter namedFilter = dialog.getNamedFilter();

        namedFilterRegistry.addFilter(namedFilter);

        updateFilterNames();
    }

    private void editNamedFilter(String name) {

        if (name == null) {
            return;
        }

        NamedFilter namedFilter = namedFilterRegistry.getFilter(name);
        OldNamedFilterEditorDialog dialog = new OldNamedFilterEditorDialog((Frame)getOwner(), namedFilterRegistry, namedFilter);
        dialog.setTitle("Edit Named Filter");
        dialog.setVisible(true);

        if (dialog.isCanceled()) {
            return;
        }

        String newName = namedFilter.getName();

        if (name.equals(newName)) {
            return;
        }

        namedFilterRegistry.removeFilter(name);
        namedFilterRegistry.addFilter(namedFilter);

        updateFilterNames();
    }

    private void deleteNamedFilter() {

        if (!confirm("Are you sure you want to delete the selected filter(s)?")) {
            return;
        }

        List<String> names = filterList.getSelectedValuesList();

        namedFilterRegistry.removeFilters(names);

        for (String name : names) {
            filterListModel.removeElement(name);
        }
    }

    private boolean confirm(String message) {
        return JOptionPane.showConfirmDialog(this, message, "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }
}
