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

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
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
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Pattern;

public class NamedFilterEditorDialog extends JDialog {

    private Pattern NAME_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9]*");

    private NamedFilterRegistry namedFilterRegistry;

    private Map<String, NamedFilter> filterMap;

    private DefaultListModel<String> filterListModel;

    private JList<String> filterList;

    private JButton removeButton;

    private JComboBox<NamedFilter.Type> typeBox;

    private JButton validateButton;

    private JTextArea textBox;

    private String selectedName;

    public NamedFilterEditorDialog(Frame owner, NamedFilterRegistry namedFilterRegistry) {

        super(owner, true);

        this.namedFilterRegistry = namedFilterRegistry;

        this.filterMap = namedFilterRegistry.getFilterMap();

        this.selectedName = null;

        setTitle("Named Filter Editor");

        setSize(getInitialSize());
        setLocationRelativeTo(owner);
        GridBagPanel mainPanel = new GridBagPanel();

        mainPanel.setBorder(new EmptyBorder(new Insets(10, 10, 10, 10)));

        filterListModel = new DefaultListModel<>();

        updateFilterNames();

        filterList = new JList<>(filterListModel);
        filterList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        filterList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                handleFilterListSelectionChanged();
            }
        });

        JButton addButton = new JButton("Add");

        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleAdd();
            }
        });

        removeButton = new JButton("Remove");

        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleRemove();
            }
        });

        GridBagPanel navButtonPanel = new GridBagPanel();
        navButtonPanel.addComponent(addButton, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0));
        navButtonPanel.addComponent(removeButton, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 6, 0, 0));

        GridBagPanel navPanel = new GridBagPanel();
        navPanel.addComponent(new JScrollPane(filterList), 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 6, 0));
        navPanel.addComponent(navButtonPanel, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0));

        typeBox = new JComboBox<>(new DefaultComboBoxModel<>(NamedFilter.Type.values()));
        typeBox.setVisible(false);

        validateButton = new JButton("Validate");
        validateButton.setVisible(false);

        validateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleValidate();
            }
        });

        textBox = new JTextArea();
        textBox.setVisible(false);

        GridBagPanel editorPanel = new GridBagPanel();
        editorPanel.setBorder(null);
        editorPanel.addComponent(typeBox, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 6, 0));
        editorPanel.addComponent(validateButton, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 6, 6, 0));
        editorPanel.addComponent(new JScrollPane(textBox), 1, 0, 2, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0));

        UIManager.put("SplitPaneDivider.border", BorderFactory.createEmptyBorder());

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(navPanel);
        splitPane.setRightComponent(editorPanel);
        splitPane.setDividerLocation((int) navButtonPanel.getPreferredSize().getWidth() + 2);
        splitPane.setBorder(null);

        JButton doneButton = new JButton("Done");

        doneButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleDone();
            }
        });

        JButton saveButton = new JButton("Save");

        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleSave();
            }
        });

        GridBagPanel mainButtonPanel = new GridBagPanel();
        mainButtonPanel.addComponent(saveButton, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0));
        mainButtonPanel.addComponent(doneButton, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 6, 0, 0));

        mainPanel.addComponent(splitPane, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0));
        mainPanel.addComponent(mainButtonPanel, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(6, 0, 0, 0));

        getContentPane().add(BorderLayout.CENTER, mainPanel);

    }

    private Dimension getInitialSize() {
        return new Dimension(540, 470);
    }

    private void updateFilterNames() {

        filterListModel.clear();

        for (String name : new TreeSet<>(filterMap.keySet())) {
            filterListModel.addElement(name);
        }
    }

    private void handleAdd() {

        String name;

        while (true) {

            name = JOptionPane.showInputDialog("Enter filter name:");

            if (name == null) {
                return;
            }

            name = name.trim();

            if (name.length() == 0) {
                error("Filter name must be specified.");
                continue;
            }

            if (!NAME_PATTERN.matcher(name).matches()) {
                error("Invalid filter name: " + name);
                continue;
            }

            if (filterMap.get(name) != null) {
                error("There is already a filter with name: " + name);
                continue;
            }

            break;
        }

        filterMap.put(name, new NamedFilter(name, NamedFilter.Type.List, "", null));

        updateFilterNames();

        filterList.setSelectedValue(name, true);
    }

    private void handleRemove() {

        if (!confirm("Are you sure you want to remove the selected named filters?")) {
            return;
        }

        List<String> selectedNames = filterList.getSelectedValuesList();

        for (String name : selectedNames) {
            filterMap.remove(name);
        }

        updateFilterNames();

        filterList.setSelectedIndex(-1);
    }

    private void handleFilterListSelectionChanged() {

        List<String> selectedNames = filterList.getSelectedValuesList();

        removeButton.setEnabled(selectedNames.size() > 0);

        updateSelectedFilter();

        if (selectedNames.size() == 1) {
            selectFilter(selectedNames.get(0));
        }
        else {
            selectFilter(null);
        }
    }

    private void selectFilter(String name) {

        selectedName = name;

        if (name == null) {
            typeBox.setVisible(false);
            textBox.setVisible(false);
            validateButton.setVisible(false);
        }
        else {
            NamedFilter namedFilter = filterMap.get(selectedName);
            typeBox.setSelectedItem(namedFilter.getType());
            textBox.setText(namedFilter.getFilterText());
            typeBox.setVisible(true);
            validateButton.setVisible(true);
            textBox.setVisible(true);
        }
    }

    private void updateSelectedFilter() {

        if (selectedName != null) {

            NamedFilter namedFilter = filterMap.get(selectedName);

            if (namedFilter != null) {
                namedFilter.setType((NamedFilter.Type) typeBox.getSelectedItem());
                namedFilter.setFilterText(textBox.getText());
            }
        }
    }

    private void handleDone() {

        updateSelectedFilter();

        if (filtersModified()) {

            int result = JOptionPane.showConfirmDialog(this, "There are unsaved changes. Save them?", "Confirm Save", JOptionPane.YES_NO_CANCEL_OPTION);

            switch (result) {

                case JOptionPane.YES_OPTION:

                    if (handleSave()) {
                        setVisible(false);
                    }

                    break;

                case JOptionPane.NO_OPTION:
                    setVisible(false);
                    break;

                case JOptionPane.CANCEL_OPTION:
                    break;
            }
        }
        else {
            setVisible(false);
        }
    }

    private boolean filtersModified() {

        if (filterMap.size() != namedFilterRegistry.size()) {
            return true;
        }

        for (Map.Entry<String, NamedFilter> entry : filterMap.entrySet()) {

            NamedFilter namedFilter = namedFilterRegistry.getFilter(entry.getKey());

            if (namedFilter == null) {
                return true;
            }

            if (!entry.getValue().getType().equals(namedFilter.getType())) {
                return true;
            }

            if (!entry.getValue().getFilterText().equals(namedFilter.getFilterText())) {
                return true;
            }
        }

        return false;
    }

    private boolean handleSave() {

        updateSelectedFilter();

        try {
            buildFilters();
        }
        catch (BuildException e) {
            // TODO Create a custom dialog, in case there are many errors.
            error(e.getMessage());
            return false;
        }

        namedFilterRegistry.setFilterMap(filterMap);

        return true;
    }

    private void handleValidate() {

        updateSelectedFilter();

        NamedFilter namedFilter = filterMap.get(selectedName);

        NamedFilter.Type type = namedFilter.getType();

        try {
            switch (type) {

                case XML:
                    namedFilter.setFilter(buildXmlFilter(namedFilter.getFilterText()));
                    break;

                case List:
                    namedFilter.setFilter(buildListFilter(namedFilter.getFilterText()));
                    break;

                default:
                    throw new BuildException("Unsupported named filter type: " + type);
            }

            info("Filter " + selectedName + " is valid.");
        }
        catch (BuildException e) {
            error(e.getMessage());
        }
    }

    private boolean confirm(String message) {
        return JOptionPane.showConfirmDialog(this, message, "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    private void error(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void info(String message) {
        JOptionPane.showMessageDialog(this, message, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    private void buildFilters() throws BuildException {

        List<String> errorMessages = new ArrayList<>();

        for (NamedFilter namedFilter : filterMap.values()) {

            try {

                NamedFilter.Type type = namedFilter.getType();

                switch (type) {

                    case XML:
                        namedFilter.setFilter(buildXmlFilter(namedFilter.getFilterText()));
                        break;

                    case List:
                        namedFilter.setFilter(buildListFilter(namedFilter.getFilterText()));
                        break;

                    default:
                        throw new BuildException("Unsupported named filter type: " + type);
                }
            }
            catch (BuildException e) {
                errorMessages.add(getFullErrorMessage(namedFilter, e.getMessage()));
            }
        }

        if (errorMessages.isEmpty()) {
            return;
        }

        StringBuilder sb = new StringBuilder();

        for (String errorMessage : errorMessages) {

            if (sb.length() > 0) {
                sb.append('\n');
            }

            sb.append(errorMessage);
        }

        throw new BuildException(sb.toString());
    }

    private String getFullErrorMessage(NamedFilter namedFilter, String errorMessage) {
        return "Error in filter " + namedFilter.getName() + " : " + errorMessage;
    }

    private Filter buildXmlFilter(String filterText) throws BuildException {

        Document document;

        try {

            InputStream in = new ByteArrayInputStream(filterText.getBytes("UTF-8"));
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

        List<Element> result = new ArrayList<>();

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

    private Filter buildListFilter(String filterText) throws BuildException {

        String[] lines = filterText.split("\n");

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
}
