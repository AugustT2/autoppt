package com.example.pptrefresh.naming;

public final class ParsedFilename {

    private final String prefix;
    private final String segment2;
    private final String segment3;
    private final String deckType;

    public ParsedFilename(String prefix, String segment2, String segment3, String deckType) {
        this.prefix = prefix;
        this.segment2 = segment2;
        this.segment3 = segment3;
        this.deckType = deckType;
    }

    public String prefix() {
        return prefix;
    }

    public String segment2() {
        return segment2;
    }

    public String segment3() {
        return segment3;
    }

    public String deckType() {
        return deckType;
    }
}
