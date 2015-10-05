package com.fourpart.logfileviewer;

import com.fourpart.logfileviewer.filter.AndFilter;
import com.fourpart.logfileviewer.filter.Filter;
import com.fourpart.logfileviewer.filter.NamedFilter;
import com.fourpart.logfileviewer.filter.NamedFilterRegistry;
import com.fourpart.logfileviewer.filter.NotFilter;
import com.fourpart.logfileviewer.filter.OrFilter;
import com.fourpart.logfileviewer.filter.SimpleFilter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class OldNamedFilterEditorDialog extends JDialog {

    private Pattern NAME_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9]*");

    private NamedFilterRegistry namedFilterRegistry;

    private NamedFilter namedFilter;

    private boolean canceled = false;

    private JTextField nameBox;

    private JComboBox<NamedFilter.Type> typeBox;

    private JTextArea textBox;

    public OldNamedFilterEditorDialog(Frame owner, NamedFilterRegistry namedFilterRegistry, NamedFilter namedFilter) {

        super(owner, true);

        this.namedFilterRegistry = namedFilterRegistry;
        this.namedFilter = namedFilter;

        setSize(getInitialSize());
        setLocationRelativeTo(owner);

        GridBagPanel mainPanel = new GridBagPanel();

        mainPanel.setBorder(new EmptyBorder(new Insets(10, 10, 10, 10)));

        nameBox = new JTextField();
        typeBox = new JComboBox<>(new DefaultComboBoxModel<>(NamedFilter.Type.values()));
        textBox = new JTextArea();

        if (namedFilter != null) {
            nameBox.setText(namedFilter.getName());
            typeBox.setSelectedItem(namedFilter.getType());
            textBox.setText(namedFilter.getFilterText());
        }

        JButton okButton = new JButton("OK");

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleOkButton();
            }
        });

        JButton cancelButton = new JButton("Cancel");

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleCancelButton();
            }
        });

        GridBagPanel buttonStrip = new GridBagPanel();
        buttonStrip.addComponent(cancelButton, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 6));
        buttonStrip.addComponent(okButton, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0));

        mainPanel.addComponent(new JLabel("Name:"), 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 6));
        mainPanel.addComponent(nameBox, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 6));
        mainPanel.addComponent(new JLabel("Type:"), 0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 6));
        mainPanel.addComponent(typeBox, 0, 3, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0));
        mainPanel.addComponent(new JScrollPane(textBox), 1, 0, 4, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(6, 0, 6, 0));
        mainPanel.addComponent(buttonStrip, 2, 0, 4, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0));

        getContentPane().add(BorderLayout.CENTER, mainPanel);
    }

    private Dimension getInitialSize() {
        return new Dimension(500, 400);
    }

    public boolean isCanceled() {
        return canceled;
    }

    public NamedFilter getNamedFilter() {
        return namedFilter;
    }

    private void handleOkButton() {

        String name = nameBox.getText().trim();

        if (name.length() == 0) {
            error("Filter name cannot be empty.");
            return;
        }

        if (!NAME_PATTERN.matcher(name).matches()) {
            error("Invalid filter name: " + name);
            return;
        }

        if (namedFilter != null) {

            String oldName = namedFilter.getName();

            if (!oldName.equals(name)) {

                if (namedFilterRegistry.getFilter(name) != null) {
                    error("There is already an existing filter with name: " + name);
                    return;
                }
            }
        }

        NamedFilter.Type type = (NamedFilter.Type)typeBox.getSelectedItem();

        Filter filter;

        switch (type) {

            case XML: {

                try {
                    filter = buildXmlFilter();
                }
                catch (BuildException e) {
                    error(e.getMessage());
                    return;
                }

                break;
            }

            case List: {

                try {
                    filter = buildListFilter();
                }
                catch (BuildException e) {
                    error(e.getMessage());
                    return;
                }

                break;
            }

            default:
                error("Unsupported named filter type: " + type);
                return;
        }

        if (namedFilter == null) {
            namedFilter = new NamedFilter(name, type, textBox.getText(), filter);
        }
        else {
            namedFilter.setName(name);
            namedFilter.setType(type);
            namedFilter.setFilterText(textBox.getText());
            namedFilter.setFilter(filter);
        }

        canceled = false;
        setVisible(false);
    }

    private Filter buildXmlFilter() throws BuildException {

        Document document;

        try {

            InputStream in = new ByteArrayInputStream(textBox.getText().getBytes("UTF-8"));
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            document = documentBuilder.parse(in);
        }
        catch (Exception e) {
            throw new BuildException("Invalid XML format: " + e.getMessage());
        }

        Element rootElem = document.getDocumentElement();
        rootElem.normalize();

        return buildElement(rootElem);
     }

    private Filter buildElement(Element elem) throws BuildException {

        String tagName = elem.getTagName();

        if ("simple".equalsIgnoreCase(tagName)) {
            return buildSimple(elem);
        }

        if ("regex".equalsIgnoreCase(tagName)) {
            return buildRegEx(elem);
        }

        if ("and".equalsIgnoreCase(tagName)) {
            return buildAnd(elem);
        }

        if ("or".equalsIgnoreCase(tagName)) {
            return buildOr(elem);
        }

        if ("not".equalsIgnoreCase(tagName)) {
            return buildNot(elem);
        }

        throw new BuildException("Unrecognized element type: " + tagName);
    }

    private List<Element> getChildElements(Element elem) {

        List<Element> result = new ArrayList<Element>();

        NodeList childNodes = elem.getChildNodes();

        for (int i = 0; i < childNodes.getLength(); i++) {

            Node childNode = childNodes.item(i);

            if (childNode instanceof Element) {
                result.add((Element)childNode);
            }
        }

        return result;
    }

    private Filter buildSimple(Element elem) throws BuildException {

        List<Element> childElems = getChildElements(elem);

        if (childElems.size() > 0) {
            throw new BuildException("Element <simple> can not have any child elements.");
        }

        return new SimpleFilter(elem.getTextContent());
    }

    private Filter buildRegEx(Element elem) throws BuildException {

        List<Element> childElems = getChildElements(elem);

        if (childElems.size() > 0) {
            throw new BuildException("Element <regex> can not have any child elements.");
        }

        return new SimpleFilter(elem.getTextContent());
    }

    private Filter buildAnd(Element elem) throws BuildException {

        List<Element> childElems = getChildElements(elem);

        if (childElems.size() < 1) {
            throw new BuildException("Element <and> must have one or more child elements.");
        }

        AndFilter result = new AndFilter();

        for (Element childElem : childElems) {
            result.addFilter(buildElement(childElem));
        }

        return result;
    }

    private Filter buildOr(Element elem) throws BuildException {

        List<Element> childElems = getChildElements(elem);

        if (childElems.size() < 1) {
            throw new BuildException("Element <or> must have one or more child elements.");
        }

        OrFilter result = new OrFilter();

        for (Element childElem : childElems) {
            result.addFilter(buildElement(childElem));
        }

        return result;
    }

    private Filter buildNot(Element elem) throws BuildException {

        List<Element> childElems = getChildElements(elem);

        if (childElems.size() != 1) {
            throw new BuildException("Element <not> must have exactly one child node.");
        }

        return new NotFilter(buildElement(childElems.get(0)));
    }

    private Filter buildListFilter() throws BuildException {

        String text = textBox.getText();

        String[] lines = text.split("\n");

        OrFilter result = new OrFilter();

        for (String line : lines) {

            if (line.length() == 0) {
                continue;
            }

            result.addFilter(new SimpleFilter(line));
        }

        return result;
    }

    private class BuildException extends Exception {

        private BuildException(String message) {
            super(message);
        }
    }

    private void handleCancelButton() {
        canceled = true;
        setVisible(false);
    }

    private void error(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}

