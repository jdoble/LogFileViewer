package com.fourpart.logfileviewer;

import com.fourpart.logfileviewer.filter.NamedFilterRegistry;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.ProgressMonitor;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class LogFileViewerPanel extends GridBagPanel {

    private static final int MAX_SEARCH_TAB = 10;

    private JFrame frame;

    private NamedFilterRegistry namedFilterRegistry;

    private JLabel fileStatusLabel;
    private JProgressBar fileLoadProgressBar;
    private JPanel fileStatusFillerPanel;
    private JMenuItem fileViewerCopyItem;
    private JMenuItem fileViewerSelectAllItem;
    private JMenuItem fileViewerGoToLineItem;
    private JButton fileReloadButton;

    private TextViewer fileViewer;
    private MultiFileModel fileViewerModel;

    private JTabbedPane searchTabbedPane;

    private SearchPanel[] searchPanels;

    private ProgressMonitor progressMonitor;

    private List<Listener> listenerList = new ArrayList<>();

    private CopySwingWorker copySwingWorker = null;

    private boolean closePending = false;

    public LogFileViewerPanel(JFrame frame, NamedFilterRegistry namedFilterRegistry) {

        this.frame = frame;

        this.namedFilterRegistry = namedFilterRegistry;

        // Configure the File Viewer Panel

        fileViewerModel = new MultiFileModel();

        fileStatusLabel = new JLabel();
        fileStatusLabel.setVisible(false);

        fileLoadProgressBar = new JProgressBar(0, 100);
        fileLoadProgressBar.setVisible(false);

        fileStatusFillerPanel = new JPanel();
        fileStatusFillerPanel.setVisible(true);

        fileReloadButton = new JButton("Reload");
        fileReloadButton.setEnabled(false);
        fileReloadButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                doReloadFile();
            }
        });

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

        filePanel.addComponent(fileStatusPanel, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(0, 10, 0, 10));
        filePanel.addComponent(fileScrollPane, 1, 0, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(0, 10, 0, 10));

        // Search Panels

        searchTabbedPane = new JTabbedPane();

        searchPanels = new SearchPanel[MAX_SEARCH_TAB];

        for (int i = 0; i < searchPanels.length; i++) {

            searchPanels[i] = new SearchPanel(this, new LoadedFileInfo() {

                @Override
                public MultiFileModel getFileViewerModel() {
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
        //splitPane.setDividerSize(2);

        addComponent(splitPane, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0));
    }

    private void createFileViewerPopupMenu(Component... components) {

        final JPopupMenu popupMenu = new JPopupMenu();

        fileViewerCopyItem = new JMenuItem("Copy");
        fileViewerCopyItem.setEnabled(false);

        fileViewerCopyItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                doCopySelectionToClipboard(fileViewer);
            }
        });

        popupMenu.add(fileViewerCopyItem);

        fileViewerSelectAllItem = new JMenuItem("Select All");
        fileViewerSelectAllItem.setEnabled(false);

        fileViewerSelectAllItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                doSelectAll(fileViewer);
            }
        });

        popupMenu.add(fileViewerSelectAllItem);

        popupMenu.addSeparator();

        fileViewerGoToLineItem = new JMenuItem("Go To Line...");
        fileViewerGoToLineItem.setEnabled(false);

        fileViewerGoToLineItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                doGoToLine();
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

    public JFrame getFrame() {
        return frame;
    }

    public void doSelectAll(TextViewer textViewer) {
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        textViewer.selectAll();
        frame.setCursor(Cursor.getDefaultCursor());
    }

    public void doCopySelectionToClipboard(TextViewer textViewer) {

        copySwingWorker = new CopySwingWorker(new CopySwingWorker.Client() {

            @Override
            public void handleCopyStart() {
                frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                progressMonitor = new ProgressMonitor(frame, "Copying selected lines...", "", 0, 100);
            }

            @Override
            public boolean isCanceled() {
                return progressMonitor.isCanceled() || closePending;
            }

            @Override
            public void handleCopyProgress(int progress) {
                progressMonitor.setProgress(progress);
            }

            @Override
            public void handleCopyFinished(String errorMessage) {

                copySwingWorker = null;

                progressMonitor.close();

                frame.setCursor(Cursor.getDefaultCursor());

                if (closePending) {
                    fileViewerModel.close();
                }
                else if (errorMessage != null) {
                    error(errorMessage);
                }
            }
        }, textViewer);

        copySwingWorker.execute();
    }

    public void doGoToLine() {

        String lineNumberString = JOptionPane.showInputDialog(frame, "Enter line number", "Go To Line", JOptionPane.PLAIN_MESSAGE);

        if (lineNumberString == null) {
            return;
        }

        int lineNumber;

        try {
            lineNumber = Integer.parseInt(lineNumberString);
        } catch (NumberFormatException e) {
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

    public void doReloadFile() {
        try {
            loadFiles(null);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadFiles(final File[] files) throws FileNotFoundException {

        fileViewerModel.loadFiles(files, new MultiFileModel.Client() {

            private boolean firstFile = true;

            @Override
            public void handleLoadFileStart(File file) {

                fileStatusLabel.setText("Loading " + file.getAbsolutePath());

                if (firstFile) {

                    fileStatusLabel.setVisible(true);
                    fileStatusLabel.setToolTipText(null);

                    fileLoadProgressBar.setVisible(true);
                    fileStatusFillerPanel.setVisible(false);
                    fileReloadButton.setEnabled(false);

                    fileViewerCopyItem.setEnabled(false);
                    fileViewerSelectAllItem.setEnabled(false);
                    fileViewerGoToLineItem.setEnabled(false);

                    for (Listener listener : listenerList) {
                        listener.handleLoadFileStart();
                    }

                    firstFile = false;
                }
            }

            @Override
            public void handleLoadFileProgress(int progress) {
                fileLoadProgressBar.setValue(progress);
            }

            @Override
            public void handleLoadFilesFinished(final File[] files, long startTime) {

                fileLoadProgressBar.setValue(100);

                fileViewerSelectAllItem.setEnabled(true);
                fileViewerGoToLineItem.setEnabled(true);

                for (Listener listener : listenerList) {
                    listener.handleLoadFileFinished();
                }

                // Need to make the selected search panel's search button the default button.

                frame.getRootPane().setDefaultButton(searchPanels[searchTabbedPane.getSelectedIndex()].getSearchButton());

                final long elapsedTime = System.currentTimeMillis() - startTime;

                final int lineCount = fileViewerModel.getRowCount();

                final String fileSeriesName = FileSeriesHelper.getFileSeriesName(files);

                final String toolTipText = FileSeriesHelper.getFileNamesString(files);

                Timer delayTimer = new Timer(500, new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        fileLoadProgressBar.setVisible(false);
                        fileStatusFillerPanel.setVisible(true);

                        if (lineCount == 1) {
                            fileStatusLabel.setText(fileSeriesName + " (1 line, " + elapsedTime + " ms)");
                        } else {
                            fileStatusLabel.setText(fileSeriesName + " (" + lineCount + " lines, " + elapsedTime + " ms, " + (fileViewerModel.getLongestRow() + 1) + ")" );
                        }

                        fileStatusLabel.setToolTipText(toolTipText);

                        fileReloadButton.setEnabled(true);
                    }
                });

                delayTimer.setRepeats(false);

                delayTimer.start();
            }
        });
    }

    public void handleShow() {
        final int index = searchTabbedPane.getSelectedIndex();
        getRootPane().setDefaultButton(searchPanels[index].getSearchButton());
    }

    public void close() {

        if (copySwingWorker != null) {
            closePending = true;
        }
        else {
            fileViewerModel.close();
        }
    }

    public void addListener(Listener listener) {

        if (!listenerList.contains(listener)) {
            listenerList.add(listener);
        }
    }

    public void removeListener(Listener listener) {
        listenerList.remove(listener);
    }

    public void handleSelectionChanged(int selectedRow) {
        fileViewer.getSelectionModel().setSelectionInterval(selectedRow, selectedRow);
        fileViewer.scrollToCenter(selectedRow, 0);
    }

    public interface Listener {
        void handleLoadFileStart();
        void handleLoadFileFinished();
    }

    public interface LoadedFileInfo {
        MultiFileModel getFileViewerModel();
        int[] getColumnWidths();
    }

    public NamedFilterRegistry getNamedFilterRegistry() {
        return namedFilterRegistry;
    }

    private void error(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}