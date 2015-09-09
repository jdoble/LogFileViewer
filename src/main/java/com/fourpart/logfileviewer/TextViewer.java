package com.fourpart.logfileviewer;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;

public class TextViewer extends JTable {

    private int[] columnWidths;

    public TextViewer(TextViewerModel textViewerModel) {

        super(textViewerModel);

        // We use a monospace font to simplify the process of determining which line of content is longest
        setFont(new Font("Monospaced", Font.PLAIN, 12));

        // We do not want to display table headers
        setTableHeader(null);

        // This ensures that horizontal scroll bars will appear as needed
        setAutoResizeMode(AUTO_RESIZE_OFF);

        setIntercellSpacing(new Dimension(0, 0));

        setDefaultRenderer(Object.class, new CustomRenderer());
    }

    public void calculateColumnWidths() {

        TextViewerModel textViewerModel = getTextViewerModel();

        if (textViewerModel != null && textViewerModel.getRowCount() > 0) {

            columnWidths = new int[textViewerModel.getRowCount()];

            for (int column = 0; column < textViewerModel.getColumnCount(); column++) {

                int row = textViewerModel.getLongestRow(column);

                TableCellRenderer renderer = getCellRenderer(row, column);
                Component comp = renderer.getTableCellRendererComponent(this, getValueAt(row, column), false, false, row, column);

                int width = comp.getPreferredSize().width + 2;

                columnWidths[column] = width;
            }

            applyColumnWidths();
        }
    }

    public int[] getColumnWidths() {
        return columnWidths;
    }

    public void setColumnWidths(int[] columnWidths) {
        this.columnWidths = columnWidths;
        applyColumnWidths();
    }

    private void applyColumnWidths() {

        TextViewerModel textViewerModel = getTextViewerModel();

        for (int column = 0; column < textViewerModel.getColumnCount(); column++) {

            DefaultTableColumnModel colModel = (DefaultTableColumnModel) getColumnModel();

            TableColumn tableColumn = colModel.getColumn(column);

            tableColumn.setMaxWidth(columnWidths[column]);
            tableColumn.setMinWidth(columnWidths[column]);
        }
    }

    private TextViewerModel getTextViewerModel() {
        return (TextViewerModel)getModel();
    }

    public class CustomRenderer implements TableCellRenderer {

        TableCellRenderer renderer;

        Border border;

        public CustomRenderer() {

            renderer = getDefaultRenderer(Object.class);

            border = BorderFactory.createCompoundBorder();
            border = BorderFactory.createCompoundBorder(border, BorderFactory.createMatteBorder(0,0,0,4,Color.WHITE));
            border = BorderFactory.createCompoundBorder(border, BorderFactory.createMatteBorder(0,0,0,1,Color.LIGHT_GRAY));
            border = BorderFactory.createCompoundBorder(border, BorderFactory.createMatteBorder(0,0,0,6,Color.WHITE));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

            JComponent result = (JComponent)renderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (column == 0) {
                result.setBorder(border);
                ((JLabel)result).setHorizontalAlignment(JLabel.RIGHT);
            }
            else {
                ((JLabel)result).setHorizontalAlignment(JLabel.LEFT);
            }

            return result;
        }
    }
}
