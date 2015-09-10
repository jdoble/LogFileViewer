package com.fourpart.logfileviewer.filter;

import java.util.regex.Pattern;

public class RegExFilter implements Filter {

    private Pattern pattern;

    public RegExFilter(String patternString) {
        pattern = Pattern.compile(patternString);
    }

    @Override
    public boolean accept(String line) {
        return pattern.matcher(line).find();
    }
}
