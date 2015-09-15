package com.fourpart.logfileviewer;

import javax.swing.table.AbstractTableModel;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class FileViewerModel extends AbstractTableModel implements TextViewerModel {

    private File file;

    private int rowCount;

    private List<Long> rowIndex;

    private FileChannel fileChannel;

    private int rowIndexForLongestValue;

    public FileViewerModel() {
        rowIndex = new ArrayList<Long>();
        rowCount = 0;
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

        rowIndex.clear();

        int maxRow = rowCount - 1;

        rowCount = 0;

        if (maxRow >= 0) {
            fireTableRowsDeleted(0, maxRow);
        }
    }

    public void addRows(List<Long> rows) {
        rowIndex.addAll(rows);
        rowCount = rowIndex.size();
        fireTableRowsInserted(rowCount - rows.size(), rowCount - 1);
    }

    public void setRowIndexForLongestValue(int rowIndexForLongestValue) {
        this.rowIndexForLongestValue = rowIndexForLongestValue;
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

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {

        switch (columnIndex) {

            case 0:
                return Integer.toString(rowIndex);

            case 1:

                try {

                    if (rowIndex == rowCount - 1) {

                        // This is the last line in the file, but the file may have
                        // grown since we last scanned it, so we read from the start
                        // of the line to the first new line character, or the end
                        // of the file, whichever comes first.

                        StringBuilder sb = new StringBuilder();

                        Long pos = this.rowIndex.get(rowIndex);

                        ByteBuffer buf = ByteBuffer.allocate(8 * 1024);

                        while (true) {

                            long bytesRead = fileChannel.read(buf, pos);

                            if (bytesRead < 0) {
                                break;
                            }

                            buf.rewind();

                            byte[] bytes = buf.array();

                            for (int offset = 0; offset < bytesRead; offset++) {

                                char c = (char) bytes[offset];

                                if (c == '\n') {
                                    return sb.toString();
                                }

                                sb.append(c);
                            }
                        }

                        return sb.toString();
                    }
                    else {

                        Long pos = this.rowIndex.get(rowIndex);

                        int length = (int)(this.rowIndex.get(rowIndex + 1) - pos - 1);

                        ByteBuffer buf = ByteBuffer.allocate(length);

                        fileChannel.read(buf, pos);

                        buf.rewind();

                        return new String(buf.array(), 0, length);
                    }

                } catch (Exception e) {
                    return "Error: "+ e.getMessage();
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
                return rowIndex.size() - 1;

            case 1:
                return rowIndexForLongestValue;

            default:
                return 0;
        }
    }
}
