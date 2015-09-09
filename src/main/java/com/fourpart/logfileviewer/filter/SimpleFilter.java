package com.fourpart.logfileviewer.filter;

public class SimpleFilter implements Filter {

    private String matchString;

    public SimpleFilter(String matchString) {
        this.matchString = matchString;
    }

    @Override
    public boolean accept(String line) {
        return line.indexOf(matchString) >= 0;
    }
}
