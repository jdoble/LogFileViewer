package com.fourpart.logfileviewer;

import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class MultiFileModel extends AbstractTableModel implements TextViewerTableModel {

    private File[] files;

    private int rowCount;

    private FileInfo[] fileInfos;

    private int longestRowLength;
    private int longestRow;

    private long lineStartPos;

    public MultiFileModel() {
        rowCount = 0;
        longestRowLength = -1;
        longestRow = -1;
        lineStartPos = 0L;
    }

    public void setFiles(File[] files) {
        this.files = files;
    }

    public File[] getFiles() {
        return files;
    }

    public void setFileChannels(FileChannel[] fileChannels) {

        fileInfos = new FileInfo[fileChannels.length];

        for (int i = 0; i < fileInfos.length; i++) {
            fileInfos[i] = new FileInfo(fileChannels[i]);
        }
    }

    public void deleteAllRows() {

        fileInfos = null;

        int maxRow = rowCount - 1;

        rowCount = 0;
        longestRowLength = -1;
        longestRow = -1;
        lineStartPos = 0L;

        if (maxRow >= 0) {
            fireTableRowsDeleted(0, maxRow);
        }
    }

    public void addRows(int fileIndex, List<Long> rows) {

        FileInfo fileInfo = fileInfos[fileIndex];

        fileInfo.addRows(rows);

        fireTableRowsInserted(rowCount - rows.size(), rowCount - 1);
    }

    public int getLongestRow() {
        return longestRow;
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public int getRowCount() {
        return rowCount;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return "";
    }

    private RowInfo getRowInfo(int row) {

        for (FileInfo fileInfo : fileInfos) {

            RowInfo result = fileInfo.getRowInfo(row);

            if (result != null) {
                return result;
            }
        }

        return null;
    }

    @Override
    public Object getValueAt(int row, int column) {

        switch (column) {

            case 0:
                return Integer.toString(row + 1);

            case 1:

                RowInfo rowInfo = getRowInfo(row);

                FileChannel fileChannel = rowInfo.getFileChannel();

                try {
                    long startPos = rowInfo.getPos();

                    int length = rowInfo.getLineLength();

                    ByteBuffer buf = ByteBuffer.allocate(length);

                    fileChannel.read(buf, startPos);

                    buf.rewind();

                    return new String(buf.array(), 0, length);

                } catch (Exception e) {
                    return "Error: "+ e.toString();
                }

            default:
                return null;
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public int getLongestRow(int column) {

        switch (column) {

            case 0:
                return rowCount - 1;

            case 1:
                return longestRow;

            default:
                return 0;
        }
    }

    private class RowInfo {

        private FileChannel fileChannel;

        private long pos;

        private int lineLength;

        private RowInfo(FileChannel fileChannel, long pos, int lineLength) {
            this.fileChannel = fileChannel;
            this.pos = pos;
            this.lineLength = lineLength;
        }

        public FileChannel getFileChannel() {
            return fileChannel;
        }

        public long getPos() {
            return pos;
        }

        public int getLineLength() {
            return lineLength;
        }
    }

    private class FileInfo {

        private FileChannel fileChannel;

        private int firstRow;

        private long firstRowStartPos = -1L;

        private List<Long> rowList = new ArrayList<>();

        private FileInfo(FileChannel fileChannel) {
            this.fileChannel = fileChannel;
        }

        private void addRows(List<Long> rows) {

            if (rowList.isEmpty()) {
                firstRow = rowCount;
                firstRowStartPos = lineStartPos;
            }

            for (Long row : rows) {

                long startPos = rowList.isEmpty() ? firstRowStartPos : rowList.get(rowCount - 1) + 1;

                int length = (int)(row - startPos);

                if (length > longestRowLength) {
                    longestRowLength = length;
                    longestRow = rowCount;
                }

                rowList.add(row);
                lineStartPos = row + 1;

                rowCount++;
            }

            rowList.addAll(rows);
        }

        private RowInfo getRowInfo(int row) {

            if (row < firstRow) {
                return null;
            }

            int relativeRow = row - firstRow;

            if (relativeRow < rowList.size()) {

                long startPos = relativeRow > 0 ? rowList.get(relativeRow - 1) + 1 : firstRowStartPos;

                long endPos = rowList.get(relativeRow);

                int length = (int)(endPos - startPos);

                return new RowInfo(fileChannel, startPos, length);
            }

            return null;
        }
    }

    public interface LoadFileListener {
        void handleLoadFileStart(File file);
        void handleLoadFileProgress(int progress);
        void handleLoadFileFinished(File file, long startTime);
    }

    public void loadFile(File file, LoadFileListener listener) {
        new LoadFileSwingWorker(file, listener).execute();
    }

    // TODO make this multi-file
    private class LoadFileSwingWorker extends SwingWorker<Void, Long> {

        private static final int BUF_SIZE = 16 * 1024;

        private long startTime = System.currentTimeMillis();

        private File file;

        private LoadFileListener listener;

        private FileChannel fileChannel;

        private LoadFileSwingWorker(File file, final LoadFileListener listener) {

            this.file = file;

            this.listener = listener;

            addPropertyChangeListener(new PropertyChangeListener() {

                @Override
                public void propertyChange(final PropertyChangeEvent event) {

                    if ("progress".equals(event.getPropertyName())) {
                        listener.handleLoadFileProgress((Integer) event.getNewValue());
                    }
                }
            });

            listener.handleLoadFileStart(file);
        }

        @Override
        protected Void doInBackground() throws Exception {

            FileInputStream in = new FileInputStream(file);

            fileChannel = in.getChannel();

            setFiles(new File[] {file});
            setFileChannels(new FileChannel[] {fileChannel});

            ByteBuffer buf = ByteBuffer.allocate(BUF_SIZE);

            long readPos = 0L; // Position in the file from which we should read next

            long lastPos = -1L; // Position of the most recently published line

            long fileSize = fileChannel.size();

            while (fileChannel.read(buf, readPos) != -1) {

                int bytesRead = buf.position();
                buf.rewind();
                byte[] byteArray = buf.array();

                for (int i = 0; i < bytesRead; i++) {

                    byte c = byteArray[i];

                    if (c == '\n') {

                        long pos = readPos + i;

                        publish(pos);

                        lastPos = pos;
                    }
                }

                readPos += bytesRead;

                setProgress((int) ((readPos * 100L) / fileSize));
            }

            if (readPos > lastPos + 1) {
                publish(readPos);
            }

            return null;
        }

        @Override
        public void process(List<Long> rows) {
            addRows(0, rows);
        }

        @Override
        public void done() {
            listener.handleLoadFileFinished(file, startTime);
        }
    }
}
