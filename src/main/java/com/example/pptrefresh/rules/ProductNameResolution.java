package com.example.pptrefresh.rules;

import java.util.ArrayList;
import java.util.List;

/** deck YAML 顶层 {@code productNameResolution} 块。 */
public class ProductNameResolution {

    private String strategy;
    private Integer slideIndex;
    private String anchorText;
    private String pattern;
    private List<Integer> slideIndexes = new ArrayList<>();
    private String hint;
    private String literal;

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public ProductNameStrategy strategyEnum() {
        return ProductNameStrategy.fromYaml(strategy);
    }

    public Integer getSlideIndex() {
        return slideIndex;
    }

    public void setSlideIndex(Integer slideIndex) {
        this.slideIndex = slideIndex;
    }

    public String getAnchorText() {
        return anchorText;
    }

    public void setAnchorText(String anchorText) {
        this.anchorText = anchorText;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public List<Integer> getSlideIndexes() {
        return slideIndexes;
    }

    public void setSlideIndexes(List<Integer> slideIndexes) {
        this.slideIndexes = slideIndexes != null ? slideIndexes : new ArrayList<>();
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    public String getLiteral() {
        return literal;
    }

    public void setLiteral(String literal) {
        this.literal = literal;
    }
}
