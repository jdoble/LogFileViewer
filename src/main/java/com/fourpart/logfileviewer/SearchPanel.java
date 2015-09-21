package com.fourpart.logfileviewer;

import com.fourpart.logfileviewer.filter.AcceptAllFilter;
import com.fourpart.logfileviewer.filter.AndFilter;
import com.fourpart.logfileviewer.filter.AndNotFilter;
import com.fourpart.logfileviewer.filter.Filter;
import com.fourpart.logfileviewer.filter.OrFilter;
import com.fourpart.logfileviewer.filter.RegExFilter;
import com.fourpart.logfileviewer.filter.SimpleFilter;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;

public class SearchPanel extends GridBagPanel implements LogFileViewer.Listener {

    private static final String FILTER_BOX_PROTOTYPE_TEXT = "wwwwwwwwwwwwwwww";

    private LogFileViewer logFileViewer;

    private LogFileViewer.LoadedFileInfo loadedFileInfo;

    private TextViewer searchResultViewer;
    private SearchResultViewerModel searchResultViewerModel;

    private JMenuItem searchResultViewerCopyItem;
    private JMenuItem searchResultViewerSelectAllItem;

    private JComboBox<String> filterBox1;
    private DefaultComboBoxModel<String> filterBoxModel1;

    private JComboBox<String> filterBox2;
    private DefaultComboBoxModel<String> filterBoxModel2;

    private enum FilterOperation {
        AND, OR, AND_NOT
    }

    private JComboBox<FilterOperation> filterOperationBox;

    private JCheckBox filterRegexBox;

    private JButton searchButton;

    private JLabel searchStatusLabel;

    private JProgressBar searchProgressBar;

    private JPanel searchStatusFillerPanel;

    public SearchPanel(final LogFileViewer logFileViewer, LogFileViewer.LoadedFileInfo loadedFileInfo) {

        this.logFileViewer = logFileViewer;

        this.loadedFileInfo = loadedFileInfo;

        filterBoxModel1 = new DefaultComboBoxModel<>();
        filterBox1 = new JComboBox<>(filterBoxModel1);
        filterBox1.setEditable(true);
        filterBox1.setPrototypeDisplayValue(FILTER_BOX_PROTOTYPE_TEXT);

        filterOperationBox = new JComboBox<>(FilterOperation.values());

        filterBoxModel2 = new DefaultComboBoxModel<>();
        filterBox2 = new JComboBox<>(filterBoxModel2);
        filterBox2.setEditable(true);
        filterBox2.setPrototypeDisplayValue(FILTER_BOX_PROTOTYPE_TEXT);

        filterRegexBox = new JCheckBox("RegEx");

        searchButton = new JButton("Search");
        searchButton.setEnabled(false);
        searchButton.addActionListener(new SearchButtonActionListener());

        GridBagPanel filterControlPanel = new GridBagPanel();
        filterControlPanel.addComponent(filterBox1, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(3, 2, 2, 0));
        filterControlPanel.addComponent(filterOperationBox, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(3, 2, 2, 0));
        filterControlPanel.addComponent(filterBox2, 0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(3, 2, 2, 0));
        filterControlPanel.addComponent(new JPanel(), 0, 3, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0));
        filterControlPanel.addComponent(filterRegexBox, 0, 4, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(3, 2, 2, 4));
        filterControlPanel.addComponent(searchButton, 0, 5, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(3, 2, 2, 2));

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

                logFileViewer.handleSelectionChanged(fileRow);
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

        logFileViewer.addListener(this);
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
                logFileViewer.handleCopyStart();
                new CopySwingWorker(logFileViewer, searchResultViewer).execute();
            }
        });

        popupMenu.add(searchResultViewerCopyItem);

        searchResultViewerSelectAllItem = new JMenuItem("Select All");
        searchResultViewerSelectAllItem.setEnabled(false);

        searchResultViewerSelectAllItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                logFileViewer.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                searchResultViewer.selectAll();
                logFileViewer.setCursor(Cursor.getDefaultCursor());
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

    private class SearchButtonActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent actionEvent) {

            handleSearchFileStart();

            Filter filter = buildFilter();

            new SearchButtonSwingWorker(filter).execute();
        }

        private Filter buildFilter() {

            String searchText1 = getComboBoxValue(filterBox1, filterBoxModel1);
            String searchText2 = getComboBoxValue(filterBox2, filterBoxModel2);

            if (searchText1 == null) {

                if (searchText2 == null) {
                    return new AcceptAllFilter();
                }
                else {
                    return buildFilter(searchText2);
                }
            }
            else {

                if (searchText2 == null) {
                    return buildFilter(searchText1);
                }
                else {
                    switch((FilterOperation) filterOperationBox.getSelectedItem()) {
                        case AND:
                            return new AndFilter(buildFilter(searchText1), buildFilter(searchText2));
                        case OR:
                            return new OrFilter(buildFilter(searchText1), buildFilter(searchText2));
                        case AND_NOT:
                            return new AndNotFilter(buildFilter(searchText1), buildFilter(searchText2));
                        default:
                            return new AcceptAllFilter();
                    }
                }
            }
        }

        private Filter buildFilter(String matchText) {

            if (filterRegexBox.isSelected()) {
                return new RegExFilter(matchText);
            }
            else {
                return new SimpleFilter(matchText);
            }
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

            FileInputStream in = new FileInputStream(loadedFileInfo.getFileViewerModel().getFile());
            FileChannel fileChannel = in.getChannel();

            ByteBuffer buf = ByteBuffer.allocate(16 * 1024);

            StringBuilder sb = new StringBuilder();

            int nextIndex = 0;

            long offset = 0L;

            while (fileChannel.read(buf, offset) != -1) {

                int len = buf.position();
                buf.rewind();
                int pos = 0;
                byte[] byteArray = buf.array();

                while (pos < len) {

                    char c = (char)byteArray[pos++];

                    if (c == '\n') {

                        String line = sb.toString();

                        if (filter.accept(line)) {
                            publish(nextIndex);
                        }

                        sb.delete(0, sb.length());

                        nextIndex++;
                    }
                    else {
                        sb.append(c);
                    }
                }

                offset += len;

                setProgress((int) ((offset * 100L) / fileChannel.size()));
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
