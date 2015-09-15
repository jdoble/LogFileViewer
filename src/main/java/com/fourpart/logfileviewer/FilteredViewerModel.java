package com.fourpart.logfileviewer;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class FilteredViewerModel extends AbstractTableModel implements TextViewerModel {

    private int rowCount;

    private List<Integer> rowIndex;

    private TextViewerModel parentModel;

    public FilteredViewerModel() {
        rowIndex = new ArrayList<>();
        rowCount = 0;
    }

    public void setParentModel(TextViewerModel parentModel) {
        this.parentModel = parentModel;
    }

    public void deleteAllRows() {

        rowIndex.clear();

        int maxRow = rowCount - 1;

        rowCount = 0;

        if (maxRow >= 0) {
            fireTableRowsDeleted(0, maxRow);
        }
    }

    public void addRows(List<Integer> rows) {
        rowIndex.addAll(rows);
        rowCount = rowIndex.size();
        fireTableRowsInserted(rowCount - rows.size(), rowCount - 1);
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
        return parentModel.getValueAt(this.rowIndex.get(rowIndex), columnIndex);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public int getLongestRow(int column) {
        return 0;
    }

    public int getFileRow(int row) {
        return rowIndex.get(row);
    }
}
