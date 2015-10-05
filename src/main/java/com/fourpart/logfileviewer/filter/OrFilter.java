package com.fourpart.logfileviewer.filter;

import java.util.ArrayList;
import java.util.List;

public class OrFilter implements Filter {

    private List<Filter> filterList = new ArrayList<>();

    public OrFilter() {
        // do nothing
    }

    public OrFilter(Filter... filters) {

        for (Filter filter : filters) {
            filterList.add(filter);
        }
    }

    public void addFilter(Filter filter) {
        filterList.add(filter);
    }

    @Override
    public boolean accept(String line) {

        for (Filter filter : filterList) {

            if (filter.accept(line)) {
                return true;
            }
        }

        return false;
    }
}
