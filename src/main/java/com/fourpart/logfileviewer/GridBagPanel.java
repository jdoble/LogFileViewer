package com.fourpart.logfileviewer;

import javax.swing.*;
import java.awt.*;

@SuppressWarnings("serial")
public class GridBagPanel extends JPanel {

    GridBagConstraints constraints;

    public GridBagPanel() {
        super(new GridBagLayout());
        constraints = new GridBagConstraints();
    }

    public void addComponent(Component c, int row, int column, int width, int height, double weightx, double weighty, int anchor, int fill, Insets insets) {

        constraints.gridy = row;
        constraints.gridx = column;
        constraints.gridwidth = width;
        constraints.gridheight = height;
        constraints.weightx = weightx;
        constraints.weighty = weighty;
        constraints.anchor = anchor;
        constraints.fill = fill;
        constraints.insets = insets;

        add(c, constraints);
    }
}
