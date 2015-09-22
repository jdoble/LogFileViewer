package com.fourpart.logfileviewer;

import javax.swing.SwingWorker;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.atomic.AtomicBoolean;

public class CopySwingWorker extends SwingWorker<String, Void> {

    private Client client;

    private TextViewer textViewer;
    private int[] selectedRows;

    private AtomicBoolean cancelled = new AtomicBoolean(false);

    public CopySwingWorker(final Client client, TextViewer textViewer) {

        this.client = client;
        this.textViewer = textViewer;
        this.selectedRows = textViewer.getSelectedRows();

        addPropertyChangeListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(final PropertyChangeEvent event) {

                if ("progress".equals(event.getPropertyName())) {

                    client.handleCopyProgress((Integer) event.getNewValue());

                    if (client.isCanceled()) {
                        cancelled.set(true);
                    }
                }
            }
        });

        client.handleCopyStart();
    }

    @Override
    protected String doInBackground() throws Exception {

        try {

            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < selectedRows.length; i++) {

                if (cancelled.get()) {
                    return null;
                }

                if (sb.length() > 0) {
                    sb.append('\n');
                }

                sb.append(textViewer.getValueAt(selectedRows[i], 1));

                setProgress((i * 100) / selectedRows.length);
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
            client.handleCopyFinished(get());
        }
        catch (Exception e) {
            client.handleCopyFinished(e.toString());
        }
    }

    public interface Client {
        void handleCopyStart();
        boolean isCanceled();
        void handleCopyProgress(int progress);
        void handleCopyFinished(String errorMessage);
    }
}
