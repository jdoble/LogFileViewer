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

public class FileViewerModel extends AbstractTableModel implements TextViewerTableModel {

    private File file;

    private int rowCount;

    private List<Long> rowList;

    private FileChannel fileChannel;

    private int longestRowLength;
    private int longestRow;

    public FileViewerModel() {
        rowList = new ArrayList<>();
        rowCount = 0;
        longestRowLength = -1;
        longestRow = -1;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    public void setFileChannel(FileChannel fileChannel) {
        this.fileChannel = fileChannel;
    }

    public void deleteAllRows() {

        rowList.clear();

        int maxRow = rowCount - 1;

        rowCount = 0;

        longestRowLength = -1;
        longestRow = -1;

        if (maxRow >= 0) {
            fireTableRowsDeleted(0, maxRow);
        }
    }

    public void addRows(List<Long> rows) {

        long startPos = rowList.isEmpty() ? 0 : rowList.get(rowCount - 1) + 1;

        for (Long row : rows) {

            int length = (int)(row - startPos);

            if (length > longestRowLength) {
                longestRowLength = length;
                longestRow = rowCount;
            }

            startPos = row + 1;

            rowList.add(row);

            rowCount++;
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

    @Override
    public String getColumnName(int column) {
        return "";
    }

    @Override
    public Object getValueAt(int row, int column) {

        switch (column) {

            case 0:
                return Integer.toString(row+1);

            case 1:

                try {
                    long startPos = row == 0 ? 0L : this.rowList.get(row - 1) + 1;
                    long endPos = this.rowList.get(row);

                    int length = (int)(endPos - startPos);

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
                return rowList.size() - 1;

            case 1:
                return longestRow;

            default:
                return 0;
        }
    }

    public interface Client {
        void handleLoadFileStart(File file);
        void handleLoadFileProgress(int progress);
        void handleLoadFileFinished(File file, long startTime);
    }

    public void loadFile(File file, Client client) {
        new LoadFileSwingWorker(file, client).execute();
    }

    private class LoadFileSwingWorker extends SwingWorker<Void, Long> {

        private static final int BUF_SIZE = 16 * 1024;

        private long startTime = System.currentTimeMillis();

        private File file;

        private Client client;

        private FileChannel fileChannel;

        private LoadFileSwingWorker(File file, final Client client) {

            this.file = file;

            this.client = client;

            addPropertyChangeListener(new PropertyChangeListener() {

                @Override
                public void propertyChange(final PropertyChangeEvent event) {

                    if ("progress".equals(event.getPropertyName())) {
                        client.handleLoadFileProgress((Integer) event.getNewValue());
                    }
                }
            });

            client.handleLoadFileStart(file);
        }

        @Override
        protected Void doInBackground() throws Exception {

            FileInputStream in = new FileInputStream(file);

            fileChannel = in.getChannel();

            setFile(file);
            setFileChannel(fileChannel);

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
            addRows(rows);
        }

        @Override
        public void done() {
            client.handleLoadFileFinished(file, startTime);
        }
    }
}
