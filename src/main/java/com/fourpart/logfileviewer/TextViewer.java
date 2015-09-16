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

        // We do not want to display a table grid
        setShowGrid(false);

        // We do not want any intercell spacing, because we are trying to look like a text editor
        setIntercellSpacing(new Dimension(0, 0));

        // We use a monospace font to simplify the process of determining which line of content is longest
        setFont(new Font("Monospaced", Font.PLAIN, 12));

        // We do not want to display table headers
        setTableHeader(null);

        // This ensures that horizontal scroll bars will appear as needed
        setAutoResizeMode(AUTO_RESIZE_OFF);

        // We use a custom renderer, in order to add a border to the right-hand side of the line number cells
        setDefaultRenderer(Object.class, new CustomRenderer());
    }

    public void scrollToCenter(int row, int column) {

        // scrollRectToVisible(new Rectangle(getCellRect(row, 0, true)));

        // Instead of simply calling "scrollRectToVisible" we will use the following logic
        // that will attempt to center the selected row/column within the scroll pane

        if (!(getParent() instanceof JViewport)) {
            return;
        }

        JViewport viewport = (JViewport)getParent();
        Rectangle rect = getCellRect(row, column, true);
        Rectangle viewRect = viewport.getViewRect();
        rect.setLocation(rect.x - viewRect.x, rect.y - viewRect.y);

        int centerX = (viewRect.width - rect.width) / 2;
        int centerY = (viewRect.height - rect.height) / 2;

        if (rect.x < centerX) {
            centerX = -centerX;
        }

        if (rect.y < centerY) {
            centerY = -centerY;
        }

        rect.translate(centerX, centerY);
        viewport.scrollRectToVisible(rect);
    }

    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {

        // It is not clear why we need to set the table column's preferred width every time
        // we prepare a renderer, as opposed to just doing it once, but this seems to work
        // better on Windows systems. Otherwise, we sometimes have truncated cells (i.e.
        // with ellipses).

        Component component = super.prepareRenderer(renderer, row, column);

        TableColumn tableColumn = getColumnModel().getColumn(column);
        tableColumn.setPreferredWidth(Math.max(columnWidths[column] + getIntercellSpacing().width, tableColumn.getPreferredWidth()));

        return component;
    }

    public void calculateColumnWidths() {

        // Calculate the column widths necessary to accommodate the longest row in each column.

        TextViewerModel textViewerModel = getTextViewerModel();

        if (textViewerModel != null && textViewerModel.getRowCount() > 0) {

            columnWidths = new int[textViewerModel.getColumnCount()];

            for (int column = 0; column < columnWidths.length; column++) {

                int row = textViewerModel.getLongestRow(column);

                TableCellRenderer renderer = getCellRenderer(row, column);
                Component component = renderer.getTableCellRendererComponent(this, getValueAt(row, column), false, false, row, column);

                int width = component.getPreferredSize().width + 2;

                columnWidths[column] = width;
            }
        }
    }

    public int[] getColumnWidths() {

        /*
        for (int column = 0; column < 2; column++) {
            TableColumn tableColumn = getColumnModel().getColumn(column);
            columnWidths[column] = tableColumn.getPreferredWidth();
        }
        */

        return columnWidths;
    }

    public void setColumnWidths(int[] columnWidths) {
        this.columnWidths = columnWidths;
        //applyColumnWidths();
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
