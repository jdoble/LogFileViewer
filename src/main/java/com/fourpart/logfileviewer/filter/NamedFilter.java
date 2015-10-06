package com.fourpart.logfileviewer.filter;

public class NamedFilter {

    private String name;

    public enum Type {
        Simple_Or_List, Simple_And_List, RegEx_Or_List, RegEx_And_List, XML
    }

    private Type type;

    private String filterText;

    private Filter filter;

    public NamedFilter(String name, Type type, String filterText, Filter filter) {
        this.name = name;
        this.type = type;
        this.filterText = filterText;
        this.filter = filter;
    }

    public NamedFilter(NamedFilter namedFilter) {
        this.name = namedFilter.getName();
        this.type = namedFilter.getType();
        this.filterText = namedFilter.getFilterText();
        this.filter = namedFilter.getFilter();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getFilterText() {
        return filterText;
    }

    public void setFilterText(String filterText) {
        this.filterText = filterText;
    }

    public Filter getFilter() {
        return filter;
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
    }
}
