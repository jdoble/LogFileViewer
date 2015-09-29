package com.fourpart.logfileviewer;

import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class MultiFileModel extends AbstractTableModel implements TextViewerTableModel {

    private static final int BUF_SIZE = 16 * 1024;

    private FileWrapper[] fileWrappers;

    private int rowCount;

    private int longestRowLength;
    private int longestRow;

    private long lineStartPos;

    public MultiFileModel() {
        clear();
    }

    private void clear() {

        int maxRow = rowCount - 1;

        rowCount = 0;
        longestRowLength = -1;
        longestRow = -1;
        lineStartPos = 0L;

        if (maxRow >= 0) {
            fireTableRowsDeleted(0, maxRow);
        }
    }

    public void addRows(List<IndexAndPos> rows) {

        for (IndexAndPos indexAndPos : rows) {
            FileWrapper fileWrapper = fileWrappers[indexAndPos.getIndex()];
            fileWrapper.addRow(indexAndPos.getPos());
        }

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

    private RowInfo getRowInfo(int row) {

        // TODO: Try a binary search

        for (FileWrapper fileWrapper : fileWrappers) {

            RowInfo result = fileWrapper.getRowInfo(row);

            if (result != null) {
                return result;
            }
        }

        return null;
    }

    @Override
    public Object getValueAt(int row, int column) {

        if (column == 0) {
            return Integer.toString(row + 1);
        }

        RowInfo rowInfo = getRowInfo(row);

        try {

            FileChannel fileChannel = rowInfo.getFileChannel();

            long startPos = rowInfo.getPos();

            int length = rowInfo.getLineLength();

            ByteBuffer buf = ByteBuffer.allocate(length);

            fileChannel.read(buf, startPos);

            buf.rewind();

            return new String(buf.array(), 0, length);

        } catch (Exception e) {
            return "Error: "+ e.toString();
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

    @Override
    public String getRowMetaDataString(int row) {

        for (FileWrapper fileWrapper : fileWrappers) {

            String rowMetaData = fileWrapper.getRowMetaDataString(row);

            if (rowMetaData != null) {
                return rowMetaData;
            }
        }

        return null;
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

    private class FileWrapper {

        private File file;

        private FileChannel fileChannel;

        private int firstRow;

        private long firstRowStartPos = -1L;

        private List<Long> rowList = new ArrayList<>();

        private FileWrapper(File file) {
            this.file = file;
        }

        private File getFile() {
            return file;
        }

        private FileChannel openFileChannel() throws FileNotFoundException {
            this.fileChannel = new FileInputStream(file).getChannel();
            return this.fileChannel;
        }

        private FileChannel getFileChannel() {
            return fileChannel;
        }

        private void addRow(long row) {

            if (rowList.isEmpty()) {
                firstRow = rowCount;
                firstRowStartPos = lineStartPos;
            }

            long startPos = rowList.isEmpty() ? firstRowStartPos : firstRowStartPos + rowList.get(rowCount - firstRow - 1) + 1;

            int length = (int)(row + firstRowStartPos - startPos);

            if (length > longestRowLength) {
                longestRowLength = length;
                longestRow = rowCount;
            }

            rowList.add(row);

            lineStartPos = row + 1;

            rowCount++;
        }

        private RowInfo getRowInfo(int row) {

            if (row < firstRow) {
                return null;
            }

            int relativeRow = row - firstRow;

            if (relativeRow < rowList.size()) {

                long startPos = relativeRow == 0 ? 0L : rowList.get(relativeRow - 1) + 1;

                int length = (int)(rowList.get(relativeRow) - startPos);

                return new RowInfo(fileChannel, startPos, length);
            }

            return null;
        }

        private String getRowMetaDataString(int row) {

            if (row < firstRow) {
                return null;
            }

            int relativeRow = row - firstRow;

            if (relativeRow < rowList.size()) {

                StringBuilder sb = new StringBuilder();

                sb.append(file.getName());

                sb.append(':');

                // Note: for display purposes, we number file rows starting from "1",
                // so we need to add "1" to the relativeRow.

                sb.append(relativeRow + 1);

                return sb.toString();
            }

            return null;
        }
    }

    public interface Client {
        void handleLoadFileStart(File file);
        void handleLoadFileProgress(int progress);
        void handleLoadFilesFinished(File[] files, long startTime);
    }

    public void loadFiles(File[] files, Client client) {
        new LoadFilesSwingWorker(files, client).execute();
    }

    private class LoadFilesSwingWorker extends SwingWorker<Void, IndexAndPos> {

        private long startTime = System.currentTimeMillis();

        private Client client;

        private LoadFilesSwingWorker(File[] files, final Client client) {

            if (files == null) {

                for (int i = 0; i < fileWrappers.length; i++) {
                    fileWrappers[i] = new FileWrapper(fileWrappers[i].getFile());
                }
            }
            else {

                fileWrappers = new FileWrapper[files.length];

                for (int i = 0; i < fileWrappers.length; i++) {
                    fileWrappers[i] = new FileWrapper(files[i]);
                }
            }

            clear();

            this.client = client;

            addPropertyChangeListener(new PropertyChangeListener() {

                @Override
                public void propertyChange(final PropertyChangeEvent event) {

                    if ("progress".equals(event.getPropertyName())) {
                        client.handleLoadFileProgress((Integer) event.getNewValue());
                    }
                    else if ("file".equals(event.getPropertyName())) {
                        client.handleLoadFileStart(((File)event.getNewValue()));
                    }
                }
            });

            client.handleLoadFileStart(fileWrappers[0].getFile());
        }

        @Override
        protected Void doInBackground() throws Exception {

            long totalSize = 0L;

            for (FileWrapper fileWrapper : fileWrappers) {
                FileChannel fileChannel = fileWrapper.openFileChannel();
                totalSize += fileChannel.size();
            }

            ByteBuffer buf = ByteBuffer.allocate(BUF_SIZE);

            long totalBytesRead = 0L;

            for (int i = 0; i < fileWrappers.length; i++) {

                if (i > 0) {
                    firePropertyChange("file", fileWrappers[i-1].getFile(), fileWrappers[i].getFile());
                }

                FileChannel fileChannel = fileWrappers[i].getFileChannel();

                long readPos = 0L; // Position in the file from which we should read next

                long lastPos = -1L; // Position of the most recently published line

                while (fileChannel.read(buf, readPos) != -1) {

                    int bytesRead = buf.position();
                    buf.rewind();
                    byte[] byteArray = buf.array();

                    for (int j = 0; j < bytesRead; j++) {

                        byte c = byteArray[j];

                        if (c == '\n') {

                            long pos = readPos + j;

                            publish(new IndexAndPos(i, pos));

                            lastPos = pos;
                        }
                    }

                    readPos += bytesRead;

                    totalBytesRead += bytesRead;

                    setProgress((int) ((totalBytesRead * 100L) / totalSize));
                }

                if (readPos > lastPos + 1) {
                    publish(new IndexAndPos(i, readPos));
                }
            }

            return null;
        }

        @Override
        public void process(List<IndexAndPos> rows) {
            addRows(rows);
        }

        @Override
        public void done() {

            File[] files = new File[fileWrappers.length];

            for (int i = 0; i < files.length; i++) {
                files[i] = fileWrappers[i].getFile();
            }

            client.handleLoadFilesFinished(files, startTime);
        }
    }

    class IndexAndPos {

        private int index;
        private long pos;

        public IndexAndPos(int index, long pos) {
            this.index = index;
            this.pos = pos;
        }

        public int getIndex() {
            return index;
        }

        public long getPos() {
            return pos;
        }
    }

    public Iterator<String> lineIterator() {
        return new MultiFileLineIterator();
    }

    class MultiFileLineIterator implements Iterator<String> {

        private int fileIndex = 0;

        private FileChannel fileChannel = null;

        private long offset = 0L;

        private ByteBuffer buf = ByteBuffer.allocate(BUF_SIZE);

        private byte[] bytes = null;

        private StringBuilder sb = new StringBuilder();

        private int len = 0;

        private int pos = 0;

        private String cachedLine = null;

        private boolean finished = false;

        @Override
        public boolean hasNext() {

            if (cachedLine != null) {
                return true;
            }

            if (finished) {
                return false;
            }

            try {
                return loadCachedLine();
            }
            catch (Exception e) {
                return false;
            }
        }

        @Override
        public String next() {

            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            String result = cachedLine;

            cachedLine = null;

            return result;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        private boolean loadCachedLine() throws Exception {

            while (true) {

                // Check for more bytes in the current buffer

                if (pos < len) {

                    char c = (char) bytes[pos++];

                    if (c == '\n') {
                        cachedLine = sb.toString();
                        sb.delete(0, sb.length());
                        return true;

                    }

                    sb.append(c);

                    continue;
                }

                // Need to try to load another buffer from the current file channel

                if (fileChannel != null) {

                    if (fileChannel.read(buf, offset) < 0) {

                        // We have finished reading this file, so we need to close it
                        // and move on to the next one

                        fileChannel.close();
                        fileChannel = null;
                        fileIndex++;

                        // If the last character in the file was not a newline character, the characters after the
                        // last newline character need to be treated as a separate line.

                        if (sb.length() > 0) {
                            cachedLine = sb.toString();
                            sb.delete(0, sb.length());
                            return true;
                        }

                        continue;
                    }
                    else {

                        // We were able to read more bytes from the current file channel, so
                        // prepare to process them.

                        len = buf.position();
                        pos = 0;

                        buf.rewind();
                        bytes = buf.array();

                        offset += len;

                        continue;
                    }
                }

                // Check to see if there is another file to be opened; if so open it, otherwise we are done

                if (fileIndex < fileWrappers.length) {
                    fileChannel = new FileInputStream(fileWrappers[fileIndex].getFile()).getChannel();
                    offset = 0;
                }
                else {
                    finished = true;
                    return false;
                }
            }
        }
    }
}
