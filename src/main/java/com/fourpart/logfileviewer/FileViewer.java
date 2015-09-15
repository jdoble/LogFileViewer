package com.fourpart.logfileviewer;

import com.fourpart.logfileviewer.filter.AcceptAllFilter;
import com.fourpart.logfileviewer.filter.AndFilter;
import com.fourpart.logfileviewer.filter.AndNotFilter;
import com.fourpart.logfileviewer.filter.Filter;
import com.fourpart.logfileviewer.filter.OrFilter;
import com.fourpart.logfileviewer.filter.RegExFilter;
import com.fourpart.logfileviewer.filter.SimpleFilter;

import javax.swing.*;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.FileWriter;

public class FileViewer extends JFrame {

    private static final String FILTER_BOX_PROTOTYPE_TEXT = "wwwwwwwwwwwwwwww";

    private JFileChooser fileChooser;

    private JLabel fileStatusLabel;
    private JProgressBar fileLoadProgressBar;
    private JPanel fileStatusFillerPanel;
    private JMenuItem fileViewerCopyItem;
    private JMenuItem fileViewerSelectAllItem;
    private JButton fileReloadButton;
    private TextViewer fileViewer;
    private FileViewerModel fileViewerModel;

    private TextViewer searchResultViewer;
    private FilteredViewerModel searchResultViewerModel;

    private JMenuItem searchViewerCopyItem;
    private JMenuItem searchViewerSelectAllItem;

    private JComboBox<String> filterBox1;
    private DefaultComboBoxModel<String> filterBoxModel1;

    private JComboBox<String> filterBox2;
    private DefaultComboBoxModel<String> filterBoxModel2;

    private enum FilterOperation {
        AND, OR, AND_NOT
    }

    private JComboBox<FilterOperation> operationBox;

    private JCheckBox regexBox;

    private JButton searchButton;

    private JLabel searchLabel;

    private JProgressBar searchProgressBar;

    private JPanel searchStatusFillerPanel;

    private ProgressMonitor progressMonitor;

    public FileViewer() {

        setTitle("File Viewer");

        GraphicsConfiguration graphicsConfiguration = this.getGraphicsConfiguration();
        Rectangle screenBounds = graphicsConfiguration.getBounds();
        Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(graphicsConfiguration);

        int screenWidth = (int) screenBounds.getWidth() - screenInsets.left - screenInsets.right;
        int screenHeight = (int) screenBounds.getHeight() - screenInsets.top - screenInsets.bottom;

        setSize(screenWidth, screenHeight);
        setLocationRelativeTo(null);

        fileChooser = new JFileChooser(new File("").getAbsoluteFile());

        // File Viewer Panel

        fileViewerModel = new FileViewerModel();

        fileStatusLabel = new JLabel();
        fileStatusLabel.setVisible(false);

        fileLoadProgressBar = new JProgressBar(0, 100);
        fileLoadProgressBar.setVisible(false);

        fileStatusFillerPanel = new JPanel();
        fileStatusFillerPanel.setVisible(true);

        fileReloadButton = new JButton("Reload");
        fileReloadButton.setEnabled(false);
        fileReloadButton.addActionListener(new FileReloadActionListener());

        GridBagPanel fileStatusPanel = new GridBagPanel();
        fileStatusPanel.addComponent(fileStatusLabel, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2));
        fileStatusPanel.addComponent(fileLoadProgressBar, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 6, 2, 2));
        fileStatusPanel.addComponent(fileStatusFillerPanel, 0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0));
        fileStatusPanel.addComponent(fileReloadButton, 0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2));

        fileViewer = new TextViewer(fileViewerModel);
        fileViewer.setShowGrid(false);
        fileViewer.setIntercellSpacing(new Dimension(0, 0));

        ListSelectionModel fileViewerSelectionModel = fileViewer.getSelectionModel();
        fileViewerSelectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        fileViewerSelectionModel.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {

                int[] selectedRows = fileViewer.getSelectedRows();

                fileViewerCopyItem.setEnabled(selectedRows.length > 0);
            }
        });

        JScrollPane fileScrollPane = new JScrollPane(fileViewer, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        fileScrollPane.getViewport().setBackground(Color.white);

        createFileViewerPopupMenu(fileViewer, fileScrollPane);

        GridBagPanel filePanel = new GridBagPanel();

        filePanel.addComponent(fileStatusPanel, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0));
        filePanel.addComponent(fileScrollPane, 1, 0, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0));

        // Bottom Panel

        filterBoxModel1 = new DefaultComboBoxModel<>();
        filterBox1 = new JComboBox<>(filterBoxModel1);
        filterBox1.setEditable(true);
        filterBox1.setPrototypeDisplayValue(FILTER_BOX_PROTOTYPE_TEXT);

        operationBox = new JComboBox<>(FilterOperation.values());

        filterBoxModel2 = new DefaultComboBoxModel<>();
        filterBox2 = new JComboBox<>(filterBoxModel2);
        filterBox2.setEditable(true);
        filterBox2.setPrototypeDisplayValue(FILTER_BOX_PROTOTYPE_TEXT);

        regexBox = new JCheckBox("RegEx");

        searchButton = new JButton("Search");
        searchButton.setEnabled(false);
        searchButton.addActionListener(new SearchButtonActionListener());
        getRootPane().setDefaultButton(searchButton);

        GridBagPanel filterControlPanel = new GridBagPanel();
        filterControlPanel.addComponent(filterBox1, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(3, 2, 2, 0));
        filterControlPanel.addComponent(operationBox, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(3, 2, 2, 0));
        filterControlPanel.addComponent(filterBox2, 0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(3, 2, 2, 0));
        filterControlPanel.addComponent(new JPanel(), 0, 3, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0));
        filterControlPanel.addComponent(regexBox, 0, 4, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(3, 2, 2, 4));
        filterControlPanel.addComponent(searchButton, 0, 5, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(3, 2, 2, 2));

        searchResultViewerModel = new FilteredViewerModel();
        searchResultViewer = new TextViewer(searchResultViewerModel);
        searchResultViewer.setShowGrid(false);
        searchResultViewer.setIntercellSpacing(new Dimension(0, 0));

        ListSelectionModel searchResultViewerSelectionModel = searchResultViewer.getSelectionModel();
        searchResultViewerSelectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        searchResultViewerSelectionModel.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {

                int[] selectedRows = searchResultViewer.getSelectedRows();

                searchViewerCopyItem.setEnabled(selectedRows.length > 0);

                if (selectedRows.length != 1) {
                    return;
                }

                int fileRow = searchResultViewerModel.getFileRow(selectedRows[0]);

                fileViewer.getSelectionModel().setSelectionInterval(fileRow, fileRow);
                fileViewer.getSelectionModel().setSelectionInterval(fileRow, fileRow);
                fileViewer.scrollToCenter(fileRow, 0);
            }
        });

        JScrollPane searchScrollPane = new JScrollPane(searchResultViewer, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        searchScrollPane.getViewport().setBackground(Color.white);

        createSearchViewerPopupMenu(searchResultViewer, searchScrollPane);

        GridBagPanel searchPanel = new GridBagPanel();

        searchPanel.addComponent(filterControlPanel, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0));
        searchPanel.addComponent(searchScrollPane, 1, 0, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0));

        searchLabel = new JLabel("Searching...");
        searchLabel.setVisible(false);
        searchProgressBar = new JProgressBar(0, 100);
        searchProgressBar.setVisible(false);

        searchStatusFillerPanel = new JPanel();
        searchStatusFillerPanel.setVisible(true);

        GridBagPanel searchStatusPanel = new GridBagPanel();
        searchStatusPanel.addComponent(searchLabel, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 2, 0, 0));
        searchStatusPanel.addComponent(searchProgressBar, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(2, 6, 2, 2));
        searchStatusPanel.addComponent(searchStatusFillerPanel, 0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0));

        searchPanel.addComponent(searchStatusPanel, 2, 0, 1, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 2, 0, 0));

        // Split Pane

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setTopComponent(filePanel);
        splitPane.setBottomComponent(searchPanel);
        splitPane.setResizeWeight(0.5);
        splitPane.setDividerSize(2);

        GridBagPanel mainPanel = new GridBagPanel();

        mainPanel.addComponent(splitPane, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0));

        getContentPane().add(mainPanel);

        createMenuBar();
    }

    private void createMenuBar() {

        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");

        JMenuItem fileOpenItem = new JMenuItem("Open...");

        fileOpenItem.addActionListener(new FileOpenActionListener());

        fileMenu.add(fileOpenItem);

        fileMenu.addSeparator();

        JMenuItem fileExitItem = new JMenuItem("Exit");

        fileExitItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                System.exit(0);
            }
        });

        fileMenu.add(fileExitItem);

        menuBar.add(fileMenu);

        setJMenuBar(menuBar);
    }

    private void createFileViewerPopupMenu(Component... components) {

        final JPopupMenu popupMenu = new JPopupMenu();

        fileViewerCopyItem = new JMenuItem("Copy");
        fileViewerCopyItem.setEnabled(false);

        fileViewerCopyItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                handleCopyStart();
                new CopySwingWorker(fileViewer).execute();
            }
        });

        popupMenu.add(fileViewerCopyItem);

        fileViewerSelectAllItem = new JMenuItem("Select All");
        fileViewerSelectAllItem.setEnabled(false);

        fileViewerSelectAllItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                FileViewer.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                fileViewer.selectAll();
                FileViewer.this.setCursor(Cursor.getDefaultCursor());

            }
        });

        popupMenu.add(fileViewerSelectAllItem);

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

    private void createSearchViewerPopupMenu(Component... components) {

        final JPopupMenu popupMenu = new JPopupMenu();

        searchViewerCopyItem = new JMenuItem("Copy");
        searchViewerCopyItem.setEnabled(false);

        searchViewerCopyItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                handleCopyStart();
                new CopySwingWorker(searchResultViewer).execute();
            }
        });

        popupMenu.add(searchViewerCopyItem);

        searchViewerSelectAllItem = new JMenuItem("Select All");
        searchViewerSelectAllItem.setEnabled(false);

        searchViewerSelectAllItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                FileViewer.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                searchResultViewer.selectAll();
                FileViewer.this.setCursor(Cursor.getDefaultCursor());
            }
        });

        popupMenu.add(searchViewerSelectAllItem);

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

    private class FileOpenActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent actionEvent) {

            int returnVal = fileChooser.showOpenDialog(FileViewer.this);

            if (returnVal == JFileChooser.APPROVE_OPTION) {

                loadFile(fileChooser.getSelectedFile());
            }
        }
    }

    private class FileReloadActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent actionEvent) {

            loadFile(fileViewerModel.getFile());
        }
    }

    public void loadFile(final File file) {

        handleLoadFileStart(file);

        new LoadFileSwingWorker(file).execute();
    }

    private class LoadFileSwingWorker extends SwingWorker<Void, Long> {

        private long startTime = System.currentTimeMillis();

        private File file;

        private FileChannel fileChannel;

        private int rowIndexForLongestValue;

        private LoadFileSwingWorker(File file) {

            this.file = file;

            addPropertyChangeListener(new PropertyChangeListener() {

                @Override
                public void propertyChange(final PropertyChangeEvent event) {

                    if ("progress".equals(event.getPropertyName())) {
                        handleLoadFileProgress((Integer) event.getNewValue());
                    }
                }
            });
        }

        @Override
        protected Void doInBackground() throws Exception {

            FileInputStream in = new FileInputStream(file);

            fileChannel = in.getChannel();

            fileViewerModel.setFile(file);
            fileViewerModel.setFileChannel(fileChannel);

            int lastRowIndex = 0;

            publish(0L);

            ByteBuffer buf = ByteBuffer.allocate(16 * 1024);

            long offset = 0;

            int longestLineLength = -1;

            long lastIndex = 0L;

            long fileSize = fileChannel.size();

            while (fileChannel.read(buf, offset) != -1) {

                int len = buf.position();
                buf.rewind();
                int pos = 0;
                byte[] byteArray = buf.array();

                while (pos < len) {

                    byte c = byteArray[pos++];

                    if (c == '\n') {

                        long nextIndex = offset + pos;

                        if (nextIndex < fileSize) {
                            publish(nextIndex);
                        }

                        lastRowIndex++;

                        int lineLength = (int) (nextIndex - lastIndex);

                        if (lineLength > longestLineLength) {
                            longestLineLength = lineLength;
                            rowIndexForLongestValue = lastRowIndex;
                        }

                        lastIndex = nextIndex;
                    }
                }

                offset += len;

                setProgress((int) ((offset * 100L) / fileSize));
            }

            return null;
        }

        @Override
        public void process(List<Long> rows) {
            fileViewerModel.addRows(rows);
            fileViewerModel.setRowIndexForLongestValue(rowIndexForLongestValue);
            fileViewer.calculateColumnWidths();
        }

        @Override
        public void done() {

            fileViewer.calculateColumnWidths();

            handleLoadFileFinished(file, startTime);
        }
    }

    private void handleLoadFileStart(File file) {

        fileStatusLabel.setText("Loading "+file.getAbsolutePath());
        fileStatusLabel.setVisible(true);

        fileLoadProgressBar.setVisible(true);
        fileStatusFillerPanel.setVisible(false);
        fileReloadButton.setEnabled(false);

        fileViewerCopyItem.setEnabled(false);
        fileViewerSelectAllItem.setEnabled(false);

        fileViewerModel.deleteAllRows();
        searchResultViewerModel.deleteAllRows();
        searchButton.setEnabled(false);
        searchLabel.setVisible(false);

        searchViewerCopyItem.setEnabled(false);
        searchViewerSelectAllItem.setEnabled(false);
    }

    private void handleLoadFileProgress(int progress) {
        fileLoadProgressBar.setValue(progress);
    }

    private void handleLoadFileFinished(final File file, long startTime) {

        fileLoadProgressBar.setValue(100);

        fileViewerSelectAllItem.setEnabled(true);

        searchButton.setEnabled(true);

        final long elapsedTime = System.currentTimeMillis() - startTime;

        final int lineCount = fileViewerModel.getRowCount();

        Timer delayTimer = new Timer(500, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                fileLoadProgressBar.setVisible(false);
                fileStatusFillerPanel.setVisible(true);

                if (lineCount == 1) {
                    fileStatusLabel.setText(file.getAbsolutePath() + " (1 line, " + elapsedTime + " ms)");
                } else {
                    fileStatusLabel.setText(file.getAbsolutePath() + " (" + lineCount + " lines, " + elapsedTime + " ms)");
                }

                fileReloadButton.setEnabled(true);
            }
        });

        delayTimer.setRepeats(false);

        delayTimer.start();
    }

    private void handleSearchFileStart() {

        searchButton.setEnabled(false);
        searchLabel.setText("Searching...");
        searchLabel.setVisible(true);
        searchStatusFillerPanel.setVisible(false);
        searchProgressBar.setVisible(true);

        searchViewerCopyItem.setEnabled(false);
        searchViewerSelectAllItem.setEnabled(false);

        searchResultViewerModel.deleteAllRows();

        searchResultViewerModel.setParentModel(fileViewerModel);

        searchResultViewer.setColumnWidths(fileViewer.getColumnWidths());
    }

    private void handleSearchFileProgress(int progress) {
        searchProgressBar.setValue(progress);
    }

    private void handleSearchFileFinished(long startTime) {

        searchProgressBar.setValue(100);

        searchViewerSelectAllItem.setEnabled(true);

        int lineCount = searchResultViewerModel.getRowCount();

        long elapsedTime = System.currentTimeMillis() - startTime;

        if (lineCount == 1) {
            searchLabel.setText("Found 1 line (" + elapsedTime + " ms)");
        } else {
            searchLabel.setText("Found " + lineCount + " lines (" + elapsedTime + " ms)");
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

    private void handleCopyStart() {
        FileViewer.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        progressMonitor = new ProgressMonitor(FileViewer.this, "Copying", "", 0, 100);
    }

    private void handleCopyProgress(int progress) {
        progressMonitor.setProgress(progress);
    }

    private void handleCopyFinished(String errorMessage) {

        progressMonitor.close();

        FileViewer.this.setCursor(Cursor.getDefaultCursor());

        if (errorMessage != null) {
            JOptionPane.showMessageDialog(FileViewer.this, errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
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
                    switch((FilterOperation)operationBox.getSelectedItem()) {
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

            if (regexBox.isSelected()) {
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

            FileInputStream in = new FileInputStream(fileViewerModel.getFile());
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

    private class CopySwingWorker extends SwingWorker<String, Void> {

        private TextViewer textViewer;
        private int[] selectedRows;

        private CopySwingWorker(TextViewer textViewer) {

            this.textViewer = textViewer;
            this.selectedRows = textViewer.getSelectedRows();

            addPropertyChangeListener(new PropertyChangeListener() {

                @Override
                public void propertyChange(final PropertyChangeEvent event) {

                    if ("progress".equals(event.getPropertyName())) {
                        handleCopyProgress((Integer) event.getNewValue());
                    }
                }
            });
        }

        @Override
        protected String doInBackground() throws Exception {

            try {
                StringBuilder sb = new StringBuilder();

                for (int row = 0; row < selectedRows.length; row++) {

                    if (sb.length() > 0) {
                        sb.append('\n');
                    }

                    sb.append(textViewer.getValueAt(row, 1));

                    setProgress((row * 100) / selectedRows.length);
                }

                if (sb.length() > 0) {
                    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                    clipboard.setContents(new StringSelection(sb.toString()), null);
                }

                return null;
            }
            catch (OutOfMemoryError e) {
                return "Selection is too large to copy (not enough memory).";
            }
        }

        @Override
        public void done() {

            try {
                handleCopyFinished(get());
            }
            catch (Exception e) {
                handleCopyFinished(e.toString());
            }
        }
    }

    private static void createFile(File file, int lineCount) {

        PrintWriter out = null;

        try {

            out = new PrintWriter(new BufferedWriter(new FileWriter(file)));

            for (int i = 0; i < lineCount; i++) {
                out.println("There are " + i + " bottles of root beer in the wall.");
            }

            out.println("A short line.");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {

            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(final String args[]) {

        // createFile(new File("sample.txt"), 10000000);

        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                FileViewer mainFrame = new FileViewer();

                if (args.length > 0) {

                    try {
                        mainFrame.loadFile(new File(args[0]));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                mainFrame.setVisible(true);
            }
        });
    }
}