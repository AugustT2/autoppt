package com.example.pptrefresh.query;

/** 本基金图例与 productDisplayName 模糊比对。 */
public final class FundLabelMatcher {

    private FundLabelMatcher() {}

    public static boolean matches(String chartLabel, String productDisplayName) {
        if (chartLabel == null || productDisplayName == null) {
            return false;
        }
        String a = normalize(chartLabel);
        String b = normalize(productDisplayName);
        if (a.isEmpty() || b.isEmpty()) {
            return false;
        }
        return a.equals(b) || a.contains(b) || b.contains(a);
    }

    static String normalize(String s) {
        if (s == null) {
            return "";
        }
        return s.replaceAll("\\s+", "")
                .replace('（', '(')
                .replace('）', ')')
                .trim();
    }
}
