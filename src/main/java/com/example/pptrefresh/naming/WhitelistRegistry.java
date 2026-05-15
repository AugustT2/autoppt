package com.example.pptrefresh.naming;

import com.example.pptrefresh.exception.FailureStage;
import com.example.pptrefresh.exception.RefreshException;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class WhitelistRegistry {

    private final Set<String> allowedSegment2;
    private final Set<String> allowedSegment3;
    private final Map<String, String> deckRules;

    public WhitelistRegistry(
            List<String> allowedSegment2,
            List<String> allowedSegment3,
            Map<String, String> deckRules) {
        this.allowedSegment2 = Set.copyOf(allowedSegment2);
        this.allowedSegment3 = Set.copyOf(allowedSegment3);
        this.deckRules = Map.copyOf(deckRules);
    }

    public void validate(ParsedFilename parsed) {
        if (!allowedSegment2.contains(parsed.segment2())) {
            throw new RefreshException(
                    FailureStage.WHITELIST,
                    "SEGMENT2_NOT_ALLOWED",
                    "段 2 不在白名单: " + parsed.segment2());
        }
        if (!allowedSegment3.contains(parsed.segment3())) {
            throw new RefreshException(
                    FailureStage.WHITELIST,
                    "SEGMENT3_NOT_ALLOWED",
                    "段 3 不在白名单: " + parsed.segment3());
        }
        if (!deckRules.containsKey(parsed.deckType())) {
            throw new RefreshException(
                    FailureStage.WHITELIST,
                    "DECK_TYPE_UNKNOWN",
                    "未注册 deckType 规则: " + parsed.deckType());
        }
    }

    public String rulesFileName(String deckType) {
        return deckRules.get(deckType);
    }
}
