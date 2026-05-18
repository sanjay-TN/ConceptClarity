package com.conceptclarity.util;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class InputSanitizer {

    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\p{Cntrl}&&[^\r\n\t]]");
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");

    public String cleanText(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String cleaned = CONTROL_CHARS.matcher(value).replaceAll(" ");
        cleaned = cleaned.replace("<", "").replace(">", "");
        cleaned = MULTI_SPACE.matcher(cleaned).replaceAll(" ").trim();
        return cleaned.length() > maxLength ? cleaned.substring(0, maxLength).trim() : cleaned;
    }

    public String cleanEmail(String value) {
        return cleanText(value, 120).toLowerCase();
    }
}
