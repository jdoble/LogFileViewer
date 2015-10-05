package com.fourpart.logfileviewer;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class TextAreaInfoDialog extends JDialog {

    public TextAreaInfoDialog(Window owner, String title, String text) {

        super(owner, title, ModalityType.APPLICATION_MODAL);

        setSize(getInitialSize());
        setLocationRelativeTo(owner);
        GridBagPanel mainPanel = new GridBagPanel();

        mainPanel.setBorder(new EmptyBorder(new Insets(10, 10, 10, 10)));

        JTextArea textArea = new JTextArea(text);
        textArea.setEditable(false);

        JButton okButton = new JButton("OK");

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        mainPanel.addComponent(new JScrollPane(textArea), 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0));
        mainPanel.addComponent(okButton, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(6, 0, 0, 0));

        getContentPane().add(BorderLayout.CENTER, mainPanel);
    }

    private Dimension getInitialSize() {
        return new Dimension(500, 450);
    }

}
