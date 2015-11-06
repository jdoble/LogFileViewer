package com.fourpart.logfileviewer;

import com.fourpart.logfileviewer.filter.AndFilter;
import com.fourpart.logfileviewer.filter.Filter;
import com.fourpart.logfileviewer.filter.NamedFilter;
import com.fourpart.logfileviewer.filter.NamedFilterRegistry;
import com.fourpart.logfileviewer.filter.NotFilter;
import com.fourpart.logfileviewer.filter.OrFilter;
import com.fourpart.logfileviewer.filter.RegExFilter;
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
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
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

    private JLabel nameLabel;

    private JTextField nameBox;

    private JLabel typeLabel;

    private JComboBox<NamedFilter.Type> typeBox;

    private JTextArea textBox;

    private String selectedName;

    public NamedFilterEditorDialog(Frame owner, NamedFilterRegistry namedFilterRegistry) {
        this(owner, namedFilterRegistry, null);
    }

    public NamedFilterEditorDialog(Frame owner, NamedFilterRegistry namedFilterRegistry, String selectedName) {

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

        nameLabel = new JLabel("Name:");
        nameLabel.setVisible(false);

        nameBox = new JTextField();
        nameBox.setVisible(false);

        typeLabel = new JLabel("Type:");
        typeLabel.setVisible(false);

        typeBox = new JComboBox<>(new DefaultComboBoxModel<>(NamedFilter.Type.values()));
        typeBox.setVisible(false);

        textBox = new JTextArea();
        textBox.setVisible(false);

        GridBagPanel editorPanel = new GridBagPanel();
        editorPanel.setBorder(null);
        editorPanel.addComponent(nameLabel, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0));
        editorPanel.addComponent(nameBox, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 6, 0, 0));
        editorPanel.addComponent(typeLabel, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 0, 0, 0));
        editorPanel.addComponent(typeBox, 1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(6, 6, 0, 0));
        editorPanel.addComponent(new JScrollPane(textBox), 2, 0, 2, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(6, 0, 0, 0));

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

        JButton applyButton = new JButton("Apply");

        applyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleApply();
            }
        });

        GridBagPanel mainButtonPanel = new GridBagPanel();
        mainButtonPanel.addComponent(applyButton, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0));
        mainButtonPanel.addComponent(doneButton, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 6, 0, 0));

        mainPanel.addComponent(splitPane, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0));
        mainPanel.addComponent(mainButtonPanel, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(6, 0, 0, 0));

        getContentPane().add(BorderLayout.CENTER, mainPanel);

        filterList.setSelectedValue(selectedName, true);
    }

    private Dimension getInitialSize() {
        return new Dimension(700, 470);
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

            List<String> errorMessages = new ArrayList<>();

            if (!validateName(name, errorMessages)) {
                error(errorMessages);
                continue;
            }

            break;
        }

        filterMap.put(name, new NamedFilter(name, NamedFilter.Type.Simple_Or_List, "", null));

        updateFilterNames();

        filterList.setSelectedValue(name, true);
    }

    private boolean validateName(String name, List<String> errorMessages) {

        name = name.trim();

        if (name.length() == 0) {
            errorMessages.add("Filter name must be specified.");
            return false;
        }

        if (!NAME_PATTERN.matcher(name).matches()) {
            errorMessages.add("Invalid filter name: " + name);
            return false;
        }

        if (filterMap.get(name) != null) {
            errorMessages.add("There is already a filter with name: " + name);
            return false;
        }

        return true;
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
            nameLabel.setVisible(false);
            nameBox.setVisible(false);
            typeLabel.setVisible(false);
            typeBox.setVisible(false);
            textBox.setVisible(false);
        }
        else {
            NamedFilter namedFilter = filterMap.get(selectedName);
            nameBox.setText(selectedName);
            typeBox.setSelectedItem(namedFilter.getType());
            textBox.setText(namedFilter.getFilterText());
            nameLabel.setVisible(true);
            nameBox.setVisible(true);
            typeLabel.setVisible(true);
            typeBox.setVisible(true);
            textBox.setVisible(true);
        }
    }

    private void updateSelectedFilter() {

        if (selectedName != null) {

            NamedFilter namedFilter = filterMap.get(selectedName);

            if (namedFilter != null) {

                String name = nameBox.getText().trim();

                if (!name.equals(selectedName)) {

                    List<String> errorMessages = new ArrayList<>();

                    if (!validateName(name, errorMessages)) {
                        errorMessages.add("Name will revert to: " + selectedName);
                        error(errorMessages);
                        nameBox.setText(selectedName);
                    }
                    else {
                        namedFilter.setName(name);
                        filterMap.remove(selectedName);
                        filterMap.put(name, namedFilter);
                        filterListModel.set(filterListModel.indexOf(selectedName), name);
                        selectedName = name;
                    }
                }

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

                    if (handleApply()) {
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

    private boolean handleApply() {

        updateSelectedFilter();

        try {
            buildFilters();
        }
        catch (BuildException e) {
            new TextAreaInfoDialog(this, "Filter Errors Found", e.getMessage()).setVisible(true);
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

                case Simple_Or_List:
                    namedFilter.setFilter(buildSimpleOrListFilter(namedFilter.getFilterText()));
                    break;

                case Simple_And_List:
                    namedFilter.setFilter(buildSimpleAndListFilter(namedFilter.getFilterText()));
                    break;

                case RegEx_Or_List:
                    namedFilter.setFilter(buildRegExOrListFilter(namedFilter.getFilterText()));
                    break;

                case RegEx_And_List:
                    namedFilter.setFilter(buildRegExAndListFilter(namedFilter.getFilterText()));
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

    private void error(List<String> messages) {

        StringBuilder sb = new StringBuilder();

        for (String message : messages) {

            if (sb.length() > 0) {
                sb.append('\n');
            }

            sb.append(message);
        }

        JOptionPane.showMessageDialog(this, sb.toString(), "Error", JOptionPane.ERROR_MESSAGE);
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

                    case Simple_Or_List:
                        namedFilter.setFilter(buildSimpleOrListFilter(namedFilter.getFilterText()));
                        break;

                    case Simple_And_List:
                        namedFilter.setFilter(buildSimpleAndListFilter(namedFilter.getFilterText()));
                        break;

                    case RegEx_Or_List:
                        namedFilter.setFilter(buildRegExOrListFilter(namedFilter.getFilterText()));
                        break;

                    case RegEx_And_List:
                        namedFilter.setFilter(buildRegExAndListFilter(namedFilter.getFilterText()));
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

    private Filter buildSimpleOrListFilter(String filterText) throws BuildException {

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

    private Filter buildSimpleAndListFilter(String filterText) throws BuildException {

        String[] lines = filterText.split("\n");

        AndFilter result = new AndFilter();

        for (String line : lines) {

            if (line.length() == 0) {
                continue;
            }

            result.addFilter(new SimpleFilter(line));
        }

        return result;
    }

    private Filter buildRegExOrListFilter(String filterText) throws BuildException {

        String[] lines = filterText.split("\n");

        OrFilter result = new OrFilter();

        for (String line : lines) {

            if (line.length() == 0) {
                continue;
            }

            result.addFilter(new RegExFilter(line));
        }

        return result;
    }

    private Filter buildRegExAndListFilter(String filterText) throws BuildException {

        String[] lines = filterText.split("\n");

        AndFilter result = new AndFilter();

        for (String line : lines) {

            if (line.length() == 0) {
                continue;
            }

            result.addFilter(new RegExFilter(line));
        }

        return result;
    }

    private class BuildException extends Exception {

        private BuildException(String message) {
            super(message);
        }
    }
}
