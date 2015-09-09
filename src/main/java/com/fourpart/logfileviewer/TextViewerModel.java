package com.fourpart.logfileviewer;

import javax.swing.table.TableModel;

public interface TextViewerModel extends TableModel {
    int getLongestRow(int column);
}
