package com.demo.util;

/**
 * File 3/6 - Query builder utility
 * Taint passes through string manipulation methods
 * Key: buildCondition looks like it "processes" the input but preserves taint
 */
public class QueryHelper {

    /**
     * Builds a SQL condition - appears to sanitize but doesn't
     * Coverity tracks taint through String.replaceAll (knows it preserves taint)
     * SonarQube may lose track after multiple string operations
     */
    public static String buildCondition(String field, String value) {
        // Fake sanitization - removes comments but not injection chars
        String cleaned = stripComments(value);
        return field + " = '" + cleaned + "'";
    }

    /**
     * Strips SQL comments but does NOT prevent injection
     * The multi-step string transformation is where SonarQube loses taint
     */
    private static String stripComments(String input) {
        String step1 = input.replaceAll("/\\*.*?\\*/", "");
        String step2 = step1.replaceAll("--.*$", "");
        String step3 = step2.trim();
        return step3;  // still tainted! just without comments
    }
}
