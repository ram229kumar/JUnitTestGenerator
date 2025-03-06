package com.umich.junittestgenerator.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaParserUtil {

    public static String extractClassName(String sourceCode) {
        Pattern classPattern = Pattern.compile("class\\s+(\\w+)");
        Matcher matcher = classPattern.matcher(sourceCode);
        return matcher.find() ? matcher.group(1) : "UnknownClass";
    }

    public static String[] extractMethods(String sourceCode) {
        Pattern methodPattern = Pattern.compile("public\\s+\\w+\\s+(\\w+)\\(");
        Matcher matcher = methodPattern.matcher(sourceCode);
        return matcher.results().map(m -> m.group(1)).toArray(String[]::new);
    }
}
