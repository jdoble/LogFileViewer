package com.fourpart.logfileviewer.filter;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NamedFilterRegistry {

    private Map<String, NamedFilter> filterMap = new HashMap<>();

    private DefaultComboBoxModel<String> comboBoxModel = new DefaultComboBoxModel<>();

    public NamedFilterRegistry() {
    }


    public Set<String> getNames() {
        return filterMap.keySet();
    }

    public void addFilter(NamedFilter namedFilter) {

        String name = namedFilter.getName();

        filterMap.put(name, namedFilter);

        for (int i = 0; i < comboBoxModel.getSize(); i++) {

            if (name.compareTo(comboBoxModel.getElementAt(i)) < 0) {
                comboBoxModel.insertElementAt(name, i);
                return;
            }
        }

        comboBoxModel.addElement(name);
    }

    public void removeFilter(String name) {

        filterMap.remove(name);
        comboBoxModel.removeElement(name);
    }

    public void removeFilters(List<String> names) {

        for (String name : names) {
            removeFilter(name);
        }
    }

    public NamedFilter getFilter(String name) {
        return filterMap.get(name);
    }

    public ComboBoxModel<String> getComboBoxModel() {
        return comboBoxModel;
    }

    public int size() {
        return filterMap.size();
    }

    public Map<String, NamedFilter> getFilterMap() {

        Map<String, NamedFilter> result = new HashMap<>();

        for (Map.Entry<String, NamedFilter> entry : filterMap.entrySet()) {
            result.put(entry.getKey(), new NamedFilter(entry.getValue()));
        }

        return result;
    }

    public void setFilterMap(Map<String, NamedFilter> filterMap) {

        this.filterMap.clear();

        comboBoxModel.removeAllElements();

        for (Map.Entry<String, NamedFilter> entry : filterMap.entrySet()) {
            this.filterMap.put(entry.getKey(), new NamedFilter(entry.getValue()));
            comboBoxModel.addElement(entry.getKey());
        }
    }
}
