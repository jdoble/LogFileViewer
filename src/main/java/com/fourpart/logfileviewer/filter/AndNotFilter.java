package com.fourpart.logfileviewer.filter;

public class AndNotFilter implements Filter {

    private Filter includeFilter;
    private Filter excludeFilter;

    public AndNotFilter(Filter includeFilter, Filter excludeFilter) {
        this.includeFilter = includeFilter;
        this.excludeFilter = excludeFilter;
    }

    @Override
    public boolean accept(String line) {
        return includeFilter.accept(line) && !excludeFilter.accept(line);
    }
}
