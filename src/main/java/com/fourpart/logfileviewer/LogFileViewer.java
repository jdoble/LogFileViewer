package com.fourpart.logfileviewer;

import javax.swing.*;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.FileWriter;

public class LogFileViewer extends JFrame {

    private static final int MAX_SEARCH_TAB = 10;

    private JFileChooser fileChooser;

    private JLabel fileStatusLabel;
    private JProgressBar fileLoadProgressBar;
    private JPanel fileStatusFillerPanel;
    private JMenuItem fileViewerCopyItem;
    private JMenuItem fileViewerSelectAllItem;
    private JMenuItem fileViewerGoToLineItem;
    private JButton fileReloadButton;

    private TextViewer fileViewer;
    private FileViewerModel fileViewerModel;

    private JTabbedPane searchTabbedPane;

    private SearchPanel[] searchPanels;

    private ProgressMonitor progressMonitor;

    private List<Listener> listenerList = new ArrayList<>();

    public LogFileViewer() {

        setTitle("Log File Viewer");

        // Center the window on the main display, and use all available space

        GraphicsConfiguration graphicsConfiguration = this.getGraphicsConfiguration();
        Rectangle screenBounds = graphicsConfiguration.getBounds();
        Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(graphicsConfiguration);

        int screenWidth = (int) screenBounds.getWidth() - screenInsets.left - screenInsets.right;
        int screenHeight = (int) screenBounds.getHeight() - screenInsets.top - screenInsets.bottom;

        setSize(screenWidth, screenHeight);
        setLocationRelativeTo(null);

        fileChooser = new JFileChooser(new File("").getAbsoluteFile());

        // Configure the File Viewer Panel

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

        // Search Panels

        searchTabbedPane = new JTabbedPane();

        searchPanels = new SearchPanel[MAX_SEARCH_TAB];

        for (int i = 0; i < searchPanels.length; i++) {

            searchPanels[i] = new SearchPanel(this, new LoadedFileInfo() {

                @Override
                public FileViewerModel getFileViewerModel() {
                    return fileViewerModel;
                }

                @Override
                public int[] getColumnWidths() {
                    return fileViewer.getColumnWidths();
                }
            });

            searchTabbedPane.add(Integer.toString(i + 1), searchPanels[i]);

            searchTabbedPane.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent changeEvent) {
                    JTabbedPane sourceTabbedPane = (JTabbedPane) changeEvent.getSource();
                    int index = sourceTabbedPane.getSelectedIndex();
                    getRootPane().setDefaultButton(searchPanels[index].getSearchButton());
                }
            });
        }

        // Split Pane

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setTopComponent(filePanel);
        splitPane.setBottomComponent(searchTabbedPane);
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
                new CopySwingWorker(LogFileViewer.this, fileViewer).execute();
            }
        });

        popupMenu.add(fileViewerCopyItem);

        fileViewerSelectAllItem = new JMenuItem("Select All");
        fileViewerSelectAllItem.setEnabled(false);

        fileViewerSelectAllItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                LogFileViewer.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                fileViewer.selectAll();
                LogFileViewer.this.setCursor(Cursor.getDefaultCursor());

            }
        });

        popupMenu.add(fileViewerSelectAllItem);

        popupMenu.addSeparator();

        fileViewerGoToLineItem = new JMenuItem("Go To Line...");
        fileViewerGoToLineItem.setEnabled(false);

        fileViewerGoToLineItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {

                String lineNumberString = JOptionPane.showInputDialog(LogFileViewer.this, "Enter line number", "Go To Line", JOptionPane.PLAIN_MESSAGE);

                if (lineNumberString == null) {
                    return;
                }

                int lineNumber;

                try {
                    //int fileRow = fileViewerModel.getRowIndexForLongestValue();
                    lineNumber = Integer.parseInt(lineNumberString);
                }
                catch (NumberFormatException e) {
                    error("Invalid line number: " + lineNumberString);
                    return;
                }

                if (lineNumber <= 0 || lineNumber > fileViewerModel.getRowCount()) {
                    error("Line number is out of range: " + lineNumberString);
                    return;
                }

                int rowIndex = lineNumber - 1;

                fileViewer.getSelectionModel().setSelectionInterval(rowIndex, rowIndex);
                fileViewer.scrollToCenter(rowIndex, 0);
            }
        });

        popupMenu.add(fileViewerGoToLineItem);

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

            int returnVal = fileChooser.showOpenDialog(LogFileViewer.this);

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

                        int lineLength = (int) (nextIndex - lastIndex);

                        if (lineLength > longestLineLength) {
                            longestLineLength = lineLength;
                            rowIndexForLongestValue = lastRowIndex;
                        }

                        lastRowIndex++;

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
            handleLoadFileFinished(file, startTime);
        }
    }

    public ProgressMonitor getProgressMonitor() {
        return progressMonitor;
    }

    public void addListener(Listener listener) {

        if (!listenerList.contains(listener)) {
            listenerList.add(listener);
        }
    }

    public void removeListener(Listener listener) {
        listenerList.remove(listener);
    }

    private void handleLoadFileStart(File file) {

        fileStatusLabel.setText("Loading " + file.getAbsolutePath());
        fileStatusLabel.setVisible(true);

        fileLoadProgressBar.setVisible(true);
        fileStatusFillerPanel.setVisible(false);
        fileReloadButton.setEnabled(false);

        fileViewerCopyItem.setEnabled(false);
        fileViewerSelectAllItem.setEnabled(false);
        fileViewerGoToLineItem.setEnabled(false);

        fileViewerModel.deleteAllRows();

        for (Listener listener : listenerList) {
            listener.handleLoadFileStart();
        }
    }

    private void handleLoadFileProgress(int progress) {
        fileLoadProgressBar.setValue(progress);
    }

    private void handleLoadFileFinished(final File file, long startTime) {

        fileLoadProgressBar.setValue(100);

        fileViewer.calculateColumnWidths();

        fileViewerSelectAllItem.setEnabled(true);
        fileViewerGoToLineItem.setEnabled(true);

        for (Listener listener : listenerList) {
            listener.handleLoadFileFinished();
        }

        // Need to make the selected search panel's search button the default button.

        getRootPane().setDefaultButton(searchPanels[searchTabbedPane.getSelectedIndex()].getSearchButton());

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
                    fileStatusLabel.setText(file.getAbsolutePath() + " (" + lineCount + " lines, " + elapsedTime + " ms, " + (fileViewerModel.getRowIndexForLongestValue() + 1) + ")" );
                }

                fileReloadButton.setEnabled(true);
            }
        });

        delayTimer.setRepeats(false);

        delayTimer.start();
    }

    public void handleSelectionChanged(int selectedRow) {
        fileViewer.getSelectionModel().setSelectionInterval(selectedRow, selectedRow);
        fileViewer.scrollToCenter(selectedRow, 0);
    }

    public void handleCopyStart() {
        LogFileViewer.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        progressMonitor = new ProgressMonitor(LogFileViewer.this, "Copying selected lines...", "", 0, 100);
    }

    public void handleCopyProgress(int progress) {
        progressMonitor.setProgress(progress);
    }

    public void handleCopyFinished(String errorMessage) {

        progressMonitor.close();

        LogFileViewer.this.setCursor(Cursor.getDefaultCursor());

        if (errorMessage != null) {
            error(errorMessage);
        }
    }

    public interface Listener {
        void handleLoadFileStart();
        void handleLoadFileFinished();
    }

    public interface LoadedFileInfo {
        FileViewerModel getFileViewerModel();
        int[] getColumnWidths();
    }

    private void error(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private static void createFile(File file, int lineCount) {

        List<String> phraseList = new java.util.ArrayList<>();

        phraseList.add("the rain in Spain falls mainly on the plane");
        phraseList.add("roses are red violets are blue sugar is sweet and so are you");
        phraseList.add("a bird in the hand is worth two in the bush");

        java.util.Set<String> wordSet = new java.util.TreeSet<>();

        for (String phrase : phraseList) {
            wordSet.addAll(Arrays.asList(phrase.split("\\s+")));
        }

        String[] words = new String[wordSet.size()];

        int index = 0;

        for (String word : wordSet) {
            words[index++] = word;
        }

        java.util.Random random = new java.util.Random();

        PrintWriter out = null;

        try {

            out = new PrintWriter(new BufferedWriter(new FileWriter(file)));

            for (int i = 0; i < lineCount; i++) {

                int wordCount = random.nextInt(100);

                StringBuilder sb = new StringBuilder();

                for (int j = 0; j < wordCount; j++) {

                    if (j > 0) {
                        sb.append(' ');
                    }

                    sb.append(words[random.nextInt(words.length)]);
                }

                out.println(sb.toString());
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

    private static void usage() {
        System.out.println("Usage: java -jar LogFileViewer-<version>.jar [-createSample <file-to-create> <number-of-lines>] [<file-to-open>]");
        System.exit(0);
    }

    private static String getNextStringArgument(Iterator<String> iter, boolean remove) {

        if (!iter.hasNext()) {
            usage();
        }

        String result = iter.next();

        if (remove) {
            iter.remove();
        }

        return result;
    }

    private static int getNextIntArgument(Iterator<String> iter, boolean remove) {

        String argString = getNextStringArgument(iter, remove);

        try {
            return Integer.parseInt(argString);
        }
        catch (NumberFormatException e) {
            usage();
        }

        return 0;
    }

    public static void main(String args[]) {

        final List<String> argsList = new ArrayList<>(Arrays.asList(args));

        for (Iterator<String> iter = argsList.iterator(); iter.hasNext();) {

            String arg = iter.next();

            if ("-createSample".equalsIgnoreCase(arg)) {
                iter.remove();
                String fileName = getNextStringArgument(iter, true);
                int sampleLineCount = getNextIntArgument(iter, true);
                createFile(new File(fileName), sampleLineCount);
            }
        }

        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                LogFileViewer mainFrame = new LogFileViewer();

                if (argsList.size() > 0) {

                    try {
                        mainFrame.loadFile(new File(argsList.get(0)));
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