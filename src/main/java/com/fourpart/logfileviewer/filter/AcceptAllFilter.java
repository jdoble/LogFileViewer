package com.fourpart.logfileviewer.filter;

public class AcceptAllFilter implements Filter {

    @Override
    public boolean accept(String line) {
        return true;
    }
}
