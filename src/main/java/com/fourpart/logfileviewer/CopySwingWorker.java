package com.fourpart.logfileviewer;

import javax.swing.SwingWorker;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.atomic.AtomicBoolean;

public class CopySwingWorker extends SwingWorker<String, Void> {

    private LogFileViewer logFileViewer;

    private TextViewer textViewer;
    private int[] selectedRows;

    private AtomicBoolean cancelled = new AtomicBoolean(false);

    public CopySwingWorker(final LogFileViewer logFileViewer, TextViewer textViewer) {

        this.logFileViewer = logFileViewer;
        this.textViewer = textViewer;
        this.selectedRows = textViewer.getSelectedRows();

        addPropertyChangeListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(final PropertyChangeEvent event) {

                if ("progress".equals(event.getPropertyName())) {

                    logFileViewer.handleCopyProgress((Integer) event.getNewValue());

                    if (logFileViewer.getProgressMonitor().isCanceled()) {
                        cancelled.set(true);
                    }
                }
            }
        });
    }

    @Override
    protected String doInBackground() throws Exception {

        try {

            StringBuilder sb = new StringBuilder();

            for (int row = 0; row < selectedRows.length; row++) {

                if (cancelled.get()) {
                    return null;
                }

                if (sb.length() > 0) {
                    sb.append('\n');
                }

                sb.append(textViewer.getValueAt(row, 1));

                setProgress((row * 100) / selectedRows.length);
            }

            if (sb.length() > 0) {
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(new StringSelection(sb.toString()), null);
            }

            return null;
        }
        catch (OutOfMemoryError e) {
            return "Selection is too large to copy (not enough memory).";
        }
    }

    @Override
    public void done() {

        try {
            logFileViewer.handleCopyFinished(get());
        }
        catch (Exception e) {
            logFileViewer.handleCopyFinished(e.toString());
        }
    }
}
