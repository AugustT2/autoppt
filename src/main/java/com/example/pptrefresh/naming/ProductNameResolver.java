package com.example.pptrefresh.naming;

import com.example.pptrefresh.config.PptRefreshProperties;
import com.example.pptrefresh.exception.FailureStage;
import com.example.pptrefresh.exception.RefreshException;
import com.example.pptrefresh.funds.HardcodedFundCodeLookup;
import com.example.pptrefresh.llm.LlmProductNameExtractor;
import com.example.pptrefresh.rules.DeckRules;
import com.example.pptrefresh.rules.ProductNameResolution;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ProductNameResolver {

    private final PptRefreshProperties properties;
    private final HardcodedFundCodeLookup fundCodeLookup;
    private final LlmProductNameExtractor llmProductNameExtractor;

    public ProductNameResolver(
            PptRefreshProperties properties,
            HardcodedFundCodeLookup fundCodeLookup,
            LlmProductNameExtractor llmProductNameExtractor) {
        this.properties = properties;
        this.fundCodeLookup = fundCodeLookup;
        this.llmProductNameExtractor = llmProductNameExtractor;
    }

    public ResolvedProduct resolve(XMLSlideShow ppt, DeckRules rules) {
        ProductNameResolution cfg = rules.getProductNameResolution();
        if (cfg == null) {
            throw new RefreshException(
                    FailureStage.RULES_SCHEMA,
                    "MISSING_PRODUCT_NAME_RESOLUTION",
                    "deck 规则缺少 productNameResolution",
                    null,
                    null);
        }
        String display = resolveDisplayName(ppt, cfg);
        String code = fundCodeLookup.lookupFundCode(display);
        if (StringUtils.hasText(display) && !StringUtils.hasText(code)) {
            throw new RefreshException(
                    FailureStage.PRODUCT_NAME_RESOLVE,
                    "FUND_CODE_NOT_FOUND",
                    "展示名已解析但未配置对应基金代码（演示库无映射）: " + display,
                    null,
                    null);
        }
        return new ResolvedProduct(display, code != null ? code : "");
    }

    private String resolveDisplayName(XMLSlideShow ppt, ProductNameResolution cfg) {
        int base = properties.getSlideIndexBase();
        switch (cfg.strategyEnum()) {
            case ANCHOR_REGEX:
                validateAnchorRegex(cfg);
                int ai = cfg.getSlideIndex() != null ? cfg.getSlideIndex() : 0;
                String boxText = findUniqueAnchorText(ppt, base, ai, cfg.getAnchorText());
                Pattern p = Pattern.compile(cfg.getPattern());
                Matcher m = p.matcher(boxText);
                if (!m.find()) {
                    throw new RefreshException(
                            FailureStage.PRODUCT_NAME_RESOLVE,
                            "ANCHOR_REGEX_NO_MATCH",
                            "正则未匹配到产品名: " + cfg.getPattern(),
                            null,
                            null);
                }
                return m.group(1).trim();
            case LLM_EXTRACT:
                int li = cfg.getSlideIndex() != null ? cfg.getSlideIndex() : 0;
                String slideText = SlidePlainText.collectSlide(ppt, base, li);
                if (!StringUtils.hasText(slideText)) {
                    throw new RefreshException(
                            FailureStage.PRODUCT_NAME_RESOLVE,
                            "LLM_EXTRACT_EMPTY_SLIDE",
                            "指定页无文本，slideIndex=" + li,
                            null,
                            null);
                }
                return llmProductNameExtractor.extractLine(slideText, cfg.getHint());
            case EMPTY_OK:
                return "";
            case STATIC:
                if (!StringUtils.hasText(cfg.getLiteral())) {
                    throw new RefreshException(
                            FailureStage.RULES_SCHEMA,
                            "STATIC_LITERAL_MISSING",
                            "STATIC 策略需要 literal",
                            null,
                            null);
                }
                return cfg.getLiteral().trim();
            default:
                throw new RefreshException(
                        FailureStage.RULES_SCHEMA,
                        "UNKNOWN_STRATEGY",
                        "未知策略: " + cfg.getStrategy(),
                        null,
                        null);
        }
    }

    private static void validateAnchorRegex(ProductNameResolution cfg) {
        if (!StringUtils.hasText(cfg.getAnchorText())) {
            throw new RefreshException(
                    FailureStage.RULES_SCHEMA,
                    "ANCHOR_REGEX_CONFIG",
                    "ANCHOR_REGEX 需要 anchorText",
                    null,
                    null);
        }
        if (!StringUtils.hasText(cfg.getPattern())) {
            throw new RefreshException(
                    FailureStage.RULES_SCHEMA,
                    "ANCHOR_REGEX_CONFIG",
                    "ANCHOR_REGEX 需要 pattern",
                    null,
                    null);
        }
    }

    private static String findUniqueAnchorText(XMLSlideShow ppt, int slideIndexBase, int slideIndex, String anchor) {
        int idx = slideIndexBase + slideIndex;
        if (idx < 0 || idx >= ppt.getSlides().size()) {
            throw new RefreshException(
                    FailureStage.PRODUCT_NAME_RESOLVE,
                    "SLIDE_OUT_OF_RANGE",
                    "productNameResolution 页索引越界: " + idx,
                    null,
                    null);
        }
        List<XSLFTextShape> matches = new ArrayList<>();
        com.example.pptrefresh.document.ShapeWalker.walkDepthFirst(
                ppt.getSlides().get(idx),
                shape -> {
                    if (shape instanceof XSLFTextShape) {
                        XSLFTextShape text = (XSLFTextShape) shape;
                        String c = text.getText();
                        if (c != null && c.contains(anchor)) {
                            matches.add(text);
                        }
                    }
                });
        if (matches.isEmpty()) {
            throw new RefreshException(
                    FailureStage.PRODUCT_NAME_RESOLVE,
                    "ANCHOR_NOT_FOUND",
                    "未找到锚点文本: " + anchor,
                    null,
                    null);
        }
        if (matches.size() > 1) {
            throw new RefreshException(
                    FailureStage.PRODUCT_NAME_RESOLVE,
                    "ANCHOR_NOT_UNIQUE",
                    "锚点命中多个文本框: " + anchor,
                    null,
                    null);
        }
        String full = matches.get(0).getText();
        return full != null ? full : "";
    }
}
