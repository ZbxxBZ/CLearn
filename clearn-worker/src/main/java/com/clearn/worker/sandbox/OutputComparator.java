package com.clearn.worker.sandbox;

import org.springframework.stereotype.Component;

@Component
public class OutputComparator {
    public boolean matches(String expected, String actual) {
        return stripTrailingWhitespace(expected).equals(stripTrailingWhitespace(actual));
    }

    private String stripTrailingWhitespace(String value) {
        int end = value.length();
        while (end > 0 && Character.isWhitespace(value.charAt(end - 1))) {
            end--;
        }
        return value.substring(0, end);
    }
}
