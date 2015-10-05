package com.fourpart.logfileviewer.filter;

public class NotFilter implements Filter {

    private Filter filter;

    public NotFilter(Filter filter) {
        this.filter = filter;
    }

    @Override
    public boolean accept(String line) {
        return !filter.accept(line);
    }
}
