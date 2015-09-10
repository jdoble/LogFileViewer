package com.fourpart.logfileviewer;

import com.fourpart.logfileviewer.filter.AcceptAllFilter;
import com.fourpart.logfileviewer.filter.AndFilter;
import com.fourpart.logfileviewer.filter.Filter;
import com.fourpart.logfileviewer.filter.OrFilter;
import com.fourpart.logfileviewer.filter.RegExFilter;
import com.fourpart.logfileviewer.filter.SimpleFilter;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JButton;
import javax.swing.ListSelectionModel;
import javax.swing.JSplitPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Rectangle;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
    private	TextViewer fileViewer;
    private FileViewerModel fileViewerModel;

    private TextViewer searchResultViewer;
    private FilteredViewerModel searchResultViewerModel;

    private JComboBox<String> filterBox1;
    private DefaultComboBoxModel<String> filterBoxModel1;

    private JComboBox<String> filterBox2;
    private DefaultComboBoxModel<String> filterBoxModel2;

    private enum FilterOperation {
        AND, OR
    }

    private JComboBox<FilterOperation> operationBox;

    private JCheckBox regexBox;

    private JButton searchButton;

    private JLabel searchLabel;

    private JProgressBar searchProgressBar;

    private JPanel searchStatusFillerPanel;

    public FileViewer() {

        setTitle("File Viewer");

        setSize(1000, 600);

        fileChooser = new JFileChooser(new File("").getAbsoluteFile());

        // File Viewer Panel

        try {
            fileViewerModel = new FileViewerModel();
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }

        fileStatusLabel = new JLabel();
        fileStatusLabel.setVisible(false);

        fileLoadProgressBar = new JProgressBar(0, 100);
        fileLoadProgressBar.setVisible(false);

        fileStatusFillerPanel = new JPanel();
        fileStatusFillerPanel.setVisible(false);

        GridBagPanel fileStatusPanel = new GridBagPanel();
        fileStatusPanel.addComponent(fileStatusLabel, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2));
        fileStatusPanel.addComponent(fileLoadProgressBar, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(2, 6, 2, 2));
        fileStatusPanel.addComponent(fileStatusFillerPanel, 0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0));

        fileViewer = new TextViewer(fileViewerModel);
        fileViewer.setShowGrid(false);
        fileViewer.setIntercellSpacing(new Dimension(0, 0));

        JScrollPane fileScrollPane = new JScrollPane(fileViewer, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

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
        filterControlPanel.addComponent(filterBox1, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 2, 2, 0));
        filterControlPanel.addComponent(operationBox, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 2, 2, 0));
        filterControlPanel.addComponent(filterBox2, 0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 2, 2, 0));
        filterControlPanel.addComponent(new JPanel(), 0, 3, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0));
        filterControlPanel.addComponent(regexBox, 0, 4, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 2, 2, 0));
        filterControlPanel.addComponent(searchButton, 0, 5, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 2, 2, 0));

        searchResultViewerModel = new FilteredViewerModel();
        searchResultViewer = new TextViewer(searchResultViewerModel);
        searchResultViewer.setShowGrid(false);
        searchResultViewer.setIntercellSpacing(new Dimension(0, 0));

        ListSelectionModel searchResultViewerSelectionModel = searchResultViewer.getSelectionModel();
        searchResultViewerSelectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        searchResultViewerSelectionModel.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {

                int selectedRow = searchResultViewer.getSelectedRow();

                if (selectedRow < 0) {
                    return;
                }

                int fileRow = searchResultViewerModel.getFileRow(selectedRow);

                fileViewer.getSelectionModel().setSelectionInterval(fileRow, fileRow);
                fileViewer.getSelectionModel().setSelectionInterval(fileRow, fileRow);
                fileViewer.scrollRectToVisible(new Rectangle(fileViewer.getCellRect(fileRow, 0, true)));
            }
        });

        JScrollPane searchScrollPane = new JScrollPane(searchResultViewer, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

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
        searchStatusPanel.addComponent(searchLabel, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,2,0,0));
        searchStatusPanel.addComponent(searchProgressBar, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(2,6,2,2));
        searchStatusPanel.addComponent(searchStatusFillerPanel, 0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0,0,0,0));

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

    private class FileOpenActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent actionEvent) {

            int returnVal = fileChooser.showOpenDialog(FileViewer.this);

            if (returnVal == JFileChooser.APPROVE_OPTION) {

                loadFile(fileChooser.getSelectedFile());
            }
        }
    }

    public void loadFile(final File file) {

        fileStatusLabel.setText("Loading " + file.getAbsolutePath());
        fileStatusLabel.setVisible(true);

        fileLoadProgressBar.setVisible(true);
        fileStatusFillerPanel.setVisible(false);

        searchResultViewerModel.deleteAllRows();
        searchButton.setEnabled(false);

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
                        fileLoadProgressBar.setValue((Integer) event.getNewValue());
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

            fileLoadProgressBar.setValue(100);

            fileViewer.calculateColumnWidths();

            searchButton.setEnabled(true);

            final long elapsedTime = System.currentTimeMillis() - startTime;

            Timer delayTimer = new Timer(1000, new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    fileLoadProgressBar.setVisible(false);
                    fileStatusFillerPanel.setVisible(true);
                    fileStatusLabel.setText(file.getAbsolutePath() + " (" + elapsedTime + " ms)");
                }
            });

            delayTimer.setRepeats(false);

            delayTimer.start();
        }
    }

    private class SearchButtonActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent actionEvent) {

            searchButton.setEnabled(false);
            searchLabel.setText("Searching...");
            searchLabel.setVisible(true);
            searchStatusFillerPanel.setVisible(false);
            searchProgressBar.setVisible(true);

            searchResultViewerModel.deleteAllRows();

            searchResultViewerModel.setParentModel(fileViewerModel);

            searchResultViewer.setColumnWidths(fileViewer.getColumnWidths());

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
                            return new AndFilter(new SimpleFilter(searchText1), new SimpleFilter(searchText2));
                        case OR:
                            return new OrFilter(new SimpleFilter(searchText1), new SimpleFilter(searchText2));
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
                        searchProgressBar.setValue((Integer) event.getNewValue());
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

            searchProgressBar.setValue(100);

            int lineCount = searchResultViewerModel.getRowCount();

            long elapsedTime = System.currentTimeMillis() - startTime;

            if (lineCount == 1) {
                searchLabel.setText("Found 1 line (" + elapsedTime + " ms)");
            } else {
                searchLabel.setText("Found " + lineCount + " lines (" + elapsedTime + " ms)");
            }

            searchButton.setEnabled(true);

            Timer delayTimer = new Timer(1000, new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    searchProgressBar.setVisible(false);
                    searchStatusFillerPanel.setVisible(true);
                }
            });

            delayTimer.setRepeats(false);

            delayTimer.start();
        }
    }

    private static void createFile(File file) {

        PrintWriter out = null;

        try {

            out = new PrintWriter(new BufferedWriter(new FileWriter(file)));

            for (int i = 0; i < 10000000; i++) {
                out.println("There are " + i + " bottles of root beer in the wall.");
            }
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

        // createFile(new File("sample.txt"));

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