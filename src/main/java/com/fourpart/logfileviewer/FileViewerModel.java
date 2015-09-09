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

                    // TODO: Calculate the needed length.
                    ByteBuffer buf = ByteBuffer.allocate(8 * 1024);

                    Long pos = this.rowIndex.get(rowIndex);

                    int bytesRead = fileChannel.read(buf, pos);

                    buf.rewind();

                    if (rowIndex == rowCount - 1) {
                        return new String(buf.array(), 0, Math.max(0, bytesRead));
                    }
                    else {
                        return new String(buf.array(), 0, (int) (long) (this.rowIndex.get(rowIndex + 1) - pos));
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
