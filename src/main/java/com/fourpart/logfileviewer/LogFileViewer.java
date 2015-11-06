package com.fourpart.logfileviewer;

import com.fourpart.logfileviewer.filter.NamedFilterRegistry;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.Color;
import java.awt.Component;
import java.awt.GraphicsConfiguration;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class LogFileViewer extends JFrame {

    private JFileChooser fileChooser;

    private NamedFilterRegistry namedFilterRegistry;

    private JSplitPane splitPane;

    private JTree fileTree;

    private DefaultTreeModel fileTreeModel;

    private DefaultMutableTreeNode rootFileTreeNode;

    private JPanel emptyPanel;

    private JMenuItem fileCloseItem;

    public LogFileViewer() {

        setTitle("Log File Viewer");

        namedFilterRegistry = new NamedFilterRegistry();

        // Center the window on the main display, and use all available space

        GraphicsConfiguration graphicsConfiguration = this.getGraphicsConfiguration();
        Rectangle screenBounds = graphicsConfiguration.getBounds();
        Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(graphicsConfiguration);

        int screenWidth = (int) screenBounds.getWidth() - screenInsets.left - screenInsets.right;
        int screenHeight = (int) screenBounds.getHeight() - screenInsets.top - screenInsets.bottom;

        setSize(screenWidth, screenHeight);
        setLocationRelativeTo(null);

        fileChooser = new JFileChooser(new File("").getAbsoluteFile());

        GridBagPanel mainPanel = new GridBagPanel();

        //System.err.println(UIManager.getDefaults().get("SplitPane.border").getClass().getName());

        emptyPanel = new JPanel();
        emptyPanel.setBorder((Border)UIManager.getDefaults().get("SplitPane.border"));

        rootFileTreeNode = new DefaultMutableTreeNode("Root");

        fileTreeModel = new DefaultTreeModel(rootFileTreeNode);

        fileTree = new JTree(fileTreeModel);
        fileTree.setRootVisible(false);
        fileTree.setBackground(emptyPanel.getBackground());
        fileTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        fileTree.setCellRenderer(new DefaultTreeCellRenderer() {

            @Override
            public Color getBackgroundNonSelectionColor() {
                return null;
            }

            @Override
            public Color getBackground() {
                return null;
            }
        });

        fileTree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {

                DefaultMutableTreeNode node = (DefaultMutableTreeNode)fileTree.getLastSelectedPathComponent();

                if (node == null) {
                    setRightComponent(emptyPanel);
                    fileCloseItem.setEnabled(false);
                }
                else {

                    ComponentWrapper componentWrapper = (ComponentWrapper)node.getUserObject();
                    componentWrapper.showComponent();

                    fileCloseItem.setEnabled(true);
                }
            }
        });

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        splitPane.setLeftComponent(new JScrollPane(fileTree));
        splitPane.setRightComponent(emptyPanel);
        //splitPane.setDividerSize(2);
        splitPane.setDividerLocation(150 + splitPane.getInsets().left);

        mainPanel.addComponent(splitPane, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0));

        getContentPane().add(mainPanel);

        createMenuBar();
    }

    private void setRightComponent(Component component) {

        int dividerLocation = splitPane.getDividerLocation();
        splitPane.setRightComponent(component);
        splitPane.setDividerLocation(dividerLocation);
    }

    private void createMenuBar() {

        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");

        JMenuItem fileOpenItem = new JMenuItem("Open...");

        fileOpenItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                doFileOpen();
            }
        });

        fileMenu.add(fileOpenItem);

        JMenuItem fileOpenSeriesItem = new JMenuItem("Open Series...");

        fileOpenSeriesItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doFileOpenSeries();
            }
        });

        fileMenu.add(fileOpenSeriesItem);

        fileMenu.addSeparator();

        fileCloseItem = new JMenuItem("Close");
        fileCloseItem.setEnabled(false);

        fileCloseItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doClose();
            }
        });

        fileMenu.add(fileCloseItem);

        fileMenu.addSeparator();

        JMenuItem fileExitItem = new JMenuItem("Exit");

        fileExitItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                doExit();
            }
        });

        fileMenu.add(fileExitItem);

        menuBar.add(fileMenu);

        JMenu toolsMenu = new JMenu("Tools");

        JMenuItem toolsNamedFilterEditorItem = new JMenuItem("Named Filter Editor...");

        toolsNamedFilterEditorItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //new OldNamedFilterRegistryEditorDialog(LogFileViewer.this, namedFilterRegistry).setVisible(true);
                new NamedFilterEditorDialog(LogFileViewer.this, namedFilterRegistry).setVisible(true);
            }
        });

        toolsMenu.add(toolsNamedFilterEditorItem);

        menuBar.add(toolsMenu);

        setJMenuBar(menuBar);
    }

    public void doFileOpen() {

        fileChooser.setMultiSelectionEnabled(false);

        int returnVal = fileChooser.showOpenDialog(LogFileViewer.this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {

            try {
                loadFiles(new File[]{fileChooser.getSelectedFile()});
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void doFileOpenSeries() {

        fileChooser.setMultiSelectionEnabled(true);

        int returnVal = fileChooser.showOpenDialog(LogFileViewer.this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {

            File[] selectedFiles = fileChooser.getSelectedFiles();

            if (selectedFiles.length == 0) {
                error("No files were selected.");
                return;
            }

            try {
                loadFiles(FileSeriesHelper.getFileSeries(selectedFiles));
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void doClose() {

        DefaultMutableTreeNode node = (DefaultMutableTreeNode)fileTree.getLastSelectedPathComponent();

        if (node != null) {

            fileTreeModel.removeNodeFromParent(node);

            ComponentWrapper componentWrapper = (ComponentWrapper)node.getUserObject();
            componentWrapper.close();
        }
    }

    public void doExit() {
        System.exit(0);
    }

    public void loadFiles(final File[] files) throws FileNotFoundException {

        LogFileViewerPanelWrapper logFileViewerPanelWrapper = new LogFileViewerPanelWrapper(files);

        logFileViewerPanelWrapper.showComponent();

        DefaultMutableTreeNode fileNode = new DefaultMutableTreeNode(logFileViewerPanelWrapper);

        fileTreeModel.insertNodeInto(fileNode, rootFileTreeNode, rootFileTreeNode.getChildCount());
        fileTree.setSelectionPath(new TreePath(new Object[] {rootFileTreeNode, fileNode} ));
    }

    private void error(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private interface ComponentWrapper {
        void showComponent();
        void close();
    }

    private class LogFileViewerPanelWrapper implements ComponentWrapper {

        private String displayName;

        private LogFileViewerPanel logFileViewerPanel;

        private LogFileViewerPanelWrapper(File[] files) throws FileNotFoundException {
            this.displayName = FileSeriesHelper.getShortFileSeriesName(files);
            this.logFileViewerPanel = new LogFileViewerPanel(LogFileViewer.this, namedFilterRegistry);
            logFileViewerPanel.loadFiles(files);
        }

        @Override
        public void showComponent() {
            setRightComponent(logFileViewerPanel);
            logFileViewerPanel.handleShow();
        }

        @Override
        public void close() {
            logFileViewerPanel.close();
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private static void createFile(File file, int lineCount) {

        List<String> phraseList = new java.util.ArrayList<>();

        phraseList.add("the rain in Spain falls mainly on the plane");
        phraseList.add("roses are red violets are blue sugar is sweet and so are you");
        phraseList.add("a bird in the hand is worth two in the bush");
        phraseList.add("a stitch in time saves nine");

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

    public static void main(String args[]) throws Exception {

        //System.err.println(System.getProperty("os.name"));
        //System.err.println(UIManager.getLookAndFeel());
        //UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());

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

                    File[] filesToOpen = new File[argsList.size()];

                    for (int i = 0; i < filesToOpen.length; i++) {
                        filesToOpen[i] = new File(argsList.get(i));
                    }

                    try {
                        mainFrame.loadFiles(filesToOpen);
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