package com.example.pptrefresh.funds;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 演示用：展示名 → 基金代码。生产环境替换为数据库查询。
 */
@Component
public class HardcodedFundCodeLookup {

    private final Map<String, String> byNormalizedName = new LinkedHashMap<>();

    public HardcodedFundCodeLookup() {
        putAlias("中欧瑾添", "013998");
        putAlias("中欧瑾添A", "013998");
        putAlias("瑾添", "013998");
        putAlias("蓝海稳健增长混合A", "001234");
        putAlias("蓝海稳健增长混合", "001234");
        putAlias("演示产品", "019999");
    }

    private void putAlias(String displayName, String code) {
        byNormalizedName.put(normalize(displayName), code);
    }

    /**
     * @param displayName 解析得到的基金展示名，允许空白表示未解析
     * @return 基金代码；无映射时返回 {@code null}
     */
    public String lookupFundCode(String displayName) {
        if (!StringUtils.hasText(displayName)) {
            return null;
        }
        return byNormalizedName.get(normalize(displayName));
    }

    static String normalize(String s) {
        return s.trim().toLowerCase(Locale.ROOT).replace('\u00a0', ' ');
    }
}
