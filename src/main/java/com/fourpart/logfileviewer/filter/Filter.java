package com.fourpart.logfileviewer.filter;

public interface Filter {
    boolean accept(String line);
}
