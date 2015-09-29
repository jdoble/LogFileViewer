package com.fourpart.logfileviewer;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.border.Border;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;

public class TextViewer extends JTable {

    private boolean fixedColumnWidths = false;

    private int[] columnWidths;

    public TextViewer(TextViewerTableModel textViewerTableModel) {

        super(textViewerTableModel);

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

        // This disables automatic horizontal scrolling when a row is selected. Without this, the JTable
        // will automatically scroll horizontally so that the beginning to the column of the table cell
        // being clicked begins at the leftmost edge of the viewport, which doesn't make a lot of sense
        // when we are using a JTable as a text file viewer.
        setAutoscrolls(false);

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
    public void tableChanged(TableModelEvent tableModelEvent) {

        super.tableChanged(tableModelEvent);

        if (tableModelEvent.getType() == TableModelEvent.INSERT && !fixedColumnWidths) {

            // Calculate the column widths necessary to accommodate the longest row in each column.

            TextViewerTableModel textViewerTableModel = getTextViewerModel();

            if (textViewerTableModel != null && textViewerTableModel.getRowCount() > 0) {

                columnWidths = new int[textViewerTableModel.getColumnCount()];

                for (int column = 0; column < columnWidths.length; column++) {

                    int row = textViewerTableModel.getLongestRow(column);

                    TableCellRenderer renderer = getCellRenderer(row, column);
                    Component component = renderer.getTableCellRendererComponent(this, getValueAt(row, column), false, false, row, column);

                    int width = component.getPreferredSize().width + 2;

                    columnWidths[column] = width;
                }
            }

            applyColumnWidths();
        }
    }

    public int[] getColumnWidths() {
        return columnWidths;
    }

    public void setColumnWidths(int[] columnWidths) {
        this.fixedColumnWidths = true;
        this.columnWidths = columnWidths;
        applyColumnWidths();
    }

    private void applyColumnWidths() {

        TextViewerTableModel textViewerTableModel = getTextViewerModel();

        for (int column = 0; column < textViewerTableModel.getColumnCount(); column++) {

            DefaultTableColumnModel colModel = (DefaultTableColumnModel) getColumnModel();

            TableColumn tableColumn = colModel.getColumn(column);

            tableColumn.setMaxWidth(columnWidths[column]);
            tableColumn.setMinWidth(columnWidths[column]);
        }
    }

    private TextViewerTableModel getTextViewerModel() {
        return (TextViewerTableModel)getModel();
    }

    public class CustomRenderer implements TableCellRenderer {

        TableCellRenderer renderer;

        Border border;

        public CustomRenderer() {

            renderer = getDefaultRenderer(Object.class);

            border = BorderFactory.createCompoundBorder();
            border = BorderFactory.createCompoundBorder(border, BorderFactory.createMatteBorder(0,0,0,4,Color.WHITE));
            border = BorderFactory.createCompoundBorder(border, BorderFactory.createMatteBorder(0,0,0,1,Color.LIGHT_GRAY));
            border = BorderFactory.createCompoundBorder(border, BorderFactory.createMatteBorder(0, 0, 0, 6, Color.WHITE));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

            JComponent result = (JComponent)renderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (column == 0) {

                result.setBorder(border);

                ((JLabel)result).setHorizontalAlignment(JLabel.RIGHT);

                String rowInfo = ((TextViewerTableModel)getModel()).getRowMetaDataString(row);

                result.setToolTipText(rowInfo);
            }
            else {
                ((JLabel)result).setHorizontalAlignment(JLabel.LEFT);
                result.setToolTipText(null);
            }

            return result;
        }
    }
}
