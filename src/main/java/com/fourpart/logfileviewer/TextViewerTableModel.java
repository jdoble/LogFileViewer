package com.fourpart.logfileviewer;

import javax.swing.table.TableModel;

public interface TextViewerTableModel extends TableModel {
    int getLongestRow(int column);
    String getRowMetaDataString(int row);
}
