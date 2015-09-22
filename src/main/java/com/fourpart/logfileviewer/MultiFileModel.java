package com.fourpart.logfileviewer;

import javax.swing.table.AbstractTableModel;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class MultiFileModel extends AbstractTableModel implements TextViewerModel {

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
}
