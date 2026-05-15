package com.example.pptrefresh.naming;

/** 编排阶段解析得到的展示名 + 硬编码/库表查得的基金代码。 */
public final class ResolvedProduct {

    private final String displayName;
    private final String fundCode;

    public ResolvedProduct(String displayName, String fundCode) {
        this.displayName = displayName != null ? displayName : "";
        this.fundCode = fundCode != null ? fundCode : "";
    }

    public String displayName() {
        return displayName;
    }

    public String fundCode() {
        return fundCode;
    }
}
