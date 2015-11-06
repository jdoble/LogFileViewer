package com.fourpart.logfileviewer;

import com.fourpart.logfileviewer.filter.AcceptAllFilter;
import com.fourpart.logfileviewer.filter.AndFilter;
import com.fourpart.logfileviewer.filter.AndNotFilter;
import com.fourpart.logfileviewer.filter.Filter;
import com.fourpart.logfileviewer.filter.NamedFilterRegistry;
import com.fourpart.logfileviewer.filter.OrFilter;
import com.fourpart.logfileviewer.filter.RegExFilter;
import com.fourpart.logfileviewer.filter.SimpleFilter;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Iterator;
import java.util.List;

public class SearchPanel extends GridBagPanel implements LogFileViewerPanel.Listener {

    private static final String FILTER_BOX_PROTOTYPE_TEXT = "wwwwwwwwwwwwwwwwww";

    private LogFileViewerPanel logFileViewerPanel;

    private LogFileViewerPanel.LoadedFileInfo loadedFileInfo;

    private TextViewer searchResultViewer;
    private SearchResultViewerModel searchResultViewerModel;

    private JMenuItem searchResultViewerCopyItem;
    private JMenuItem searchResultViewerSelectAllItem;

    private enum FilterType {
        Simple, RegEx, Named
    }

    private JComboBox<FilterType> filterTypeBox1;

    private NamedFilterRegistry namedFilterRegistry;

    private JComboBox<String> simpleFilterBox1;
    private DefaultComboBoxModel<String> simpleFilterBoxModel1;

    private JComboBox<String> regexFilterBox1;
    private DefaultComboBoxModel<String> regexFilterBoxModel1;

    private JComboBox<String> namedFilterBox1;

    private JComboBox<FilterType> filterTypeBox2;

    private JComboBox<String> simpleFilterBox2;
    private DefaultComboBoxModel<String> simpleFilterBoxModel2;

    private JComboBox<String> regexFilterBox2;
    private DefaultComboBoxModel<String> regexFilterBoxModel2;

    private JComboBox<String> namedFilterBox2;

    private enum FilterOperation {
        And, Or, And_Not
    }

    private JComboBox<FilterOperation> filterOperationBox;

    private JButton searchButton;

    private JLabel searchStatusLabel;

    private JProgressBar searchProgressBar;

    private JPanel searchStatusFillerPanel;

    public SearchPanel(final LogFileViewerPanel logFileViewerPanel, LogFileViewerPanel.LoadedFileInfo loadedFileInfo) {

        this.logFileViewerPanel = logFileViewerPanel;

        this.loadedFileInfo = loadedFileInfo;

        filterTypeBox1 = new JComboBox<>(FilterType.values());
        filterTypeBox1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                FilterType filterType = (FilterType) filterTypeBox1.getSelectedItem();
                simpleFilterBox1.setVisible(filterType == FilterType.Simple);
                regexFilterBox1.setVisible(filterType == FilterType.RegEx);
                namedFilterBox1.setVisible(filterType == FilterType.Named);
            }
        });

        namedFilterRegistry = logFileViewerPanel.getNamedFilterRegistry();

        simpleFilterBoxModel1 = new DefaultComboBoxModel<>();
        simpleFilterBox1 = new JComboBox<>(simpleFilterBoxModel1);
        simpleFilterBox1.setEditable(true);
        simpleFilterBox1.setPrototypeDisplayValue(FILTER_BOX_PROTOTYPE_TEXT);
        simpleFilterBox1.setVisible(true);

        regexFilterBoxModel1 = new DefaultComboBoxModel<>();
        regexFilterBox1 = new JComboBox<>(regexFilterBoxModel1);
        regexFilterBox1.setEditable(true);
        regexFilterBox1.setPrototypeDisplayValue(FILTER_BOX_PROTOTYPE_TEXT);
        regexFilterBox1.setVisible(false);

        namedFilterBox1 = new JComboBox<>(namedFilterRegistry.getComboBoxModel());
        namedFilterBox1.setEditable(false);
        namedFilterBox1.setPrototypeDisplayValue(FILTER_BOX_PROTOTYPE_TEXT);
        namedFilterBox1.setVisible(false);

        namedFilterBox1.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {

                if (evt.getClickCount() == 2) {
                    openNamedFilterEditor((String) namedFilterBox1.getSelectedItem());
                }
            }
        });

        filterOperationBox = new JComboBox<>(FilterOperation.values());

        filterTypeBox2 = new JComboBox<>(FilterType.values());

        filterTypeBox2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                FilterType filterType = (FilterType) filterTypeBox2.getSelectedItem();
                simpleFilterBox2.setVisible(filterType == FilterType.Simple);
                regexFilterBox2.setVisible(filterType == FilterType.RegEx);
                namedFilterBox2.setVisible(filterType == FilterType.Named);
            }
        });

        simpleFilterBoxModel2 = new DefaultComboBoxModel<>();
        simpleFilterBox2 = new JComboBox<>(simpleFilterBoxModel2);
        simpleFilterBox2.setEditable(true);
        simpleFilterBox2.setPrototypeDisplayValue(FILTER_BOX_PROTOTYPE_TEXT);
        simpleFilterBox1.setVisible(true);

        regexFilterBoxModel2 = new DefaultComboBoxModel<>();
        regexFilterBox2 = new JComboBox<>(regexFilterBoxModel2);
        regexFilterBox2.setEditable(true);
        regexFilterBox2.setPrototypeDisplayValue(FILTER_BOX_PROTOTYPE_TEXT);
        regexFilterBox2.setVisible(false);

        namedFilterBox2 = new JComboBox<>(namedFilterRegistry.getComboBoxModel());
        namedFilterBox2.setEditable(false);
        namedFilterBox2.setPrototypeDisplayValue(FILTER_BOX_PROTOTYPE_TEXT);
        namedFilterBox2.setVisible(false);
        namedFilterBox2.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {

                if (evt.getClickCount() == 2) {
                    openNamedFilterEditor((String) namedFilterBox1.getSelectedItem());
                }
            }
        });

        searchButton = new JButton("Search");
        searchButton.setEnabled(false);
        searchButton.addActionListener(new SearchButtonActionListener());

        GridBagPanel filterControlPanel = new GridBagPanel();
        filterControlPanel.addComponent(filterTypeBox1, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(3, 2, 2, 0));
        filterControlPanel.addComponent(simpleFilterBox1, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(3, 2, 2, 0));
        filterControlPanel.addComponent(regexFilterBox1, 0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(3, 2, 2, 0));
        filterControlPanel.addComponent(namedFilterBox1, 0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(3, 2, 2, 0));
        filterControlPanel.addComponent(filterOperationBox, 0, 4, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(3, 16, 2, 16));
        filterControlPanel.addComponent(filterTypeBox2, 0, 5, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(3, 0, 2, 0));
        filterControlPanel.addComponent(simpleFilterBox2, 0, 6, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(3, 2, 2, 0));
        filterControlPanel.addComponent(regexFilterBox2, 0, 7, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(3, 2, 2, 0));
        filterControlPanel.addComponent(namedFilterBox2, 0, 8, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(3, 2, 2, 0));
        filterControlPanel.addComponent(new JPanel(), 0, 9, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0));
        filterControlPanel.addComponent(searchButton, 0, 10, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(3, 2, 2, 2));

        searchResultViewerModel = new SearchResultViewerModel();
        searchResultViewer = new TextViewer(searchResultViewerModel);
        searchResultViewer.setShowGrid(false);
        searchResultViewer.setIntercellSpacing(new Dimension(0, 0));

        ListSelectionModel searchResultViewerSelectionModel = searchResultViewer.getSelectionModel();
        searchResultViewerSelectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        searchResultViewerSelectionModel.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {

                int[] selectedRows = searchResultViewer.getSelectedRows();

                searchResultViewerCopyItem.setEnabled(selectedRows.length > 0);

                if (selectedRows.length != 1) {
                    return;
                }

                int fileRow = searchResultViewerModel.getFileRow(selectedRows[0]);

                logFileViewerPanel.handleSelectionChanged(fileRow);
            }
        });

        JScrollPane searchScrollPane = new JScrollPane(searchResultViewer, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        searchScrollPane.getViewport().setBackground(Color.white);

        createSearchViewerPopupMenu(searchResultViewer, searchScrollPane);

        addComponent(filterControlPanel, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0));
        addComponent(searchScrollPane, 1, 0, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0));

        searchStatusLabel = new JLabel("Searching...");
        searchStatusLabel.setVisible(false);
        searchProgressBar = new JProgressBar(0, 100);
        searchProgressBar.setVisible(false);

        searchStatusFillerPanel = new JPanel();
        searchStatusFillerPanel.setVisible(true);

        GridBagPanel searchStatusPanel = new GridBagPanel();
        searchStatusPanel.addComponent(searchStatusLabel, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 2, 0, 0));
        searchStatusPanel.addComponent(searchProgressBar, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(2, 6, 2, 2));
        searchStatusPanel.addComponent(searchStatusFillerPanel, 0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0));

        addComponent(searchStatusPanel, 2, 0, 1, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 2, 0, 0));

        logFileViewerPanel.addListener(this);
    }

    public JButton getSearchButton() {
        return searchButton;
    }

    private void createSearchViewerPopupMenu(Component... components) {

        final JPopupMenu popupMenu = new JPopupMenu();

        searchResultViewerCopyItem = new JMenuItem("Copy");
        searchResultViewerCopyItem.setEnabled(false);

        searchResultViewerCopyItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                logFileViewerPanel.doCopySelectionToClipboard(searchResultViewer);
            }
        });

        popupMenu.add(searchResultViewerCopyItem);

        searchResultViewerSelectAllItem = new JMenuItem("Select All");
        searchResultViewerSelectAllItem.setEnabled(false);

        searchResultViewerSelectAllItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                logFileViewerPanel.getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                searchResultViewer.selectAll();
                logFileViewerPanel.getFrame().setCursor(Cursor.getDefaultCursor());
            }
        });

        popupMenu.add(searchResultViewerSelectAllItem);

        for (Component component : components) {

            component.addMouseListener(new MouseAdapter() {

                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        popupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        popupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            });
        }
    }

    private void openNamedFilterEditor(String filterName) {
        new NamedFilterEditorDialog(logFileViewerPanel.getFrame(), namedFilterRegistry, filterName).setVisible(true);
    }

    private class SearchButtonActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent actionEvent) {

            handleSearchFileStart();

            Filter filter = buildFilter();

            new SearchButtonSwingWorker(filter).execute();
        }

        private Filter buildFilter() {

            Filter filter1 = buildFilter(1);

            Filter filter2 = buildFilter(2);

            if (filter1 == null) {
                return filter2 != null ? filter2 : new AcceptAllFilter();
            }

            if (filter2 == null) {
                return filter1;
            }

            switch((FilterOperation) filterOperationBox.getSelectedItem()) {
                case And:
                    return new AndFilter(filter1, filter2);
                case Or:
                    return new OrFilter(filter1, filter2);
                case And_Not:
                    return new AndNotFilter(filter1, filter2);
                default:
                    return new AcceptAllFilter();
            }
        }

        private FilterType getFilterType(int filterNumber) {

            switch (filterNumber) {
                case 1:
                    return (FilterType)filterTypeBox1.getSelectedItem();
                case 2:
                    return (FilterType)filterTypeBox2.getSelectedItem();
            }

            return null;
        }

        private String getSimpleFilterValue(int filterNumber) {

            switch (filterNumber) {
                case 1:
                    return getComboBoxValue(simpleFilterBox1, simpleFilterBoxModel1);
                case 2:
                    return getComboBoxValue(simpleFilterBox2, simpleFilterBoxModel2);
            }

            return null;
        }

        private String getRexExFilterValue(int filterNumber) {

            switch (filterNumber) {
                case 1:
                    return getComboBoxValue(regexFilterBox1, regexFilterBoxModel1);
                case 2:
                    return getComboBoxValue(regexFilterBox2, regexFilterBoxModel2);
            }

            return null;
        }

        private String getNamedFilterValue(int filterNumber) {

            if (namedFilterRegistry.size() == 0) {
                return null;
            }

            switch (filterNumber) {
                case 1:
                    return (String)namedFilterBox1.getSelectedItem();
                case 2:
                    return (String)namedFilterBox2.getSelectedItem();
            }

            return null;
        }

        private Filter buildFilter(int filterNumber) {

            FilterType filterType = getFilterType(filterNumber);

            if (filterType == null) {
                return null;
            }

            switch (filterType) {

                case Simple: {

                    String s = getSimpleFilterValue(filterNumber);

                    if (s != null) {
                        return new SimpleFilter(s);
                    }

                    break;
                }

                case RegEx: {

                    String s = getRexExFilterValue(filterNumber);

                    if (s != null) {
                        return new RegExFilter(s);
                    }

                    break;
                }

                case Named: {

                    String s = getNamedFilterValue(filterNumber);

                    if (s != null) {
                        return namedFilterRegistry.getFilter(s).getFilter();
                    }
                }
            }

            return null;
        }

        private String getComboBoxValue(JComboBox<String> comboBox, DefaultComboBoxModel<String> comboBoxModel) {

            String value = (String)comboBox.getSelectedItem();

            if (value == null || value.length() == 0) {
                return null;
            }

            comboBoxModel.removeElement(value);
            comboBoxModel.insertElementAt(value, 0);
            comboBoxModel.setSelectedItem(value);

            return value;
        }
    }

    private class SearchButtonSwingWorker extends SwingWorker<Void, Integer> {

        private long startTime = System.currentTimeMillis();

        private Filter filter;

        private SearchButtonSwingWorker(Filter filter) {

            this.filter = filter;

            addPropertyChangeListener(new PropertyChangeListener() {

                @Override
                public void propertyChange(final PropertyChangeEvent event) {

                    if ("progress".equals(event.getPropertyName())) {
                        handleSearchFileProgress((Integer) event.getNewValue());
                    }
                }
            });
        }

        @Override
        protected Void doInBackground ()throws Exception {

            int nextIndex = 0;

            int totalLines = loadedFileInfo.getFileViewerModel().getRowCount();

            for (Iterator<String> iter = loadedFileInfo.getFileViewerModel().lineIterator(); iter.hasNext();) {

                String line = iter.next();

                if (filter.accept(line)) {
                    publish(nextIndex);
                }

                nextIndex++;

                setProgress((nextIndex * 100) / totalLines);
            }

            return null;
        }

        @Override
        public void process(List<Integer> rows) {
            searchResultViewerModel.addRows(rows);
        }

        @Override
        public void done() {
            handleSearchFileFinished(startTime);
        }
    }

    private void handleSearchFileStart() {

        searchButton.setEnabled(false);
        searchStatusLabel.setText("Searching...");
        searchStatusLabel.setVisible(true);
        searchStatusFillerPanel.setVisible(false);
        searchProgressBar.setVisible(true);

        searchResultViewerCopyItem.setEnabled(false);
        searchResultViewerSelectAllItem.setEnabled(false);

        searchResultViewerModel.deleteAllRows();

        searchResultViewerModel.setParentModel(loadedFileInfo.getFileViewerModel());

        searchResultViewer.setColumnWidths(loadedFileInfo.getColumnWidths());
    }

    public void handleSearchFileProgress(int progress) {
        searchProgressBar.setValue(progress);
    }

    public void handleSearchFileFinished(long startTime) {

        searchProgressBar.setValue(100);

        searchResultViewerSelectAllItem.setEnabled(true);

        int lineCount = searchResultViewerModel.getRowCount();

        long elapsedTime = System.currentTimeMillis() - startTime;

        if (lineCount == 1) {
            searchStatusLabel.setText("Found 1 line (" + elapsedTime + " ms)");
        } else {
            searchStatusLabel.setText("Found " + lineCount + " lines (" + elapsedTime + " ms)");
        }

        searchButton.setEnabled(true);

        Timer delayTimer = new Timer(500, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                searchProgressBar.setVisible(false);
                searchStatusFillerPanel.setVisible(true);
            }
        });

        delayTimer.setRepeats(false);

        delayTimer.start();
    }

    @Override
    public void handleLoadFileStart() {

        searchResultViewerModel.deleteAllRows();
        searchButton.setEnabled(false);
        searchStatusLabel.setVisible(false);

        searchResultViewerCopyItem.setEnabled(false);
        searchResultViewerSelectAllItem.setEnabled(false);
    }

    @Override
    public void handleLoadFileFinished() {
        searchButton.setEnabled(true);
    }
}
