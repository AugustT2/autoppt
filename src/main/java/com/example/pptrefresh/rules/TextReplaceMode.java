package com.example.pptrefresh.rules;

public enum TextReplaceMode {
    after_anchor,
    replace_all,
    /** 在含 {@code fieldLabel} 的文本框内，仅替换标签后的数字部分（保留标签、单位与 Run 样式）。 */
    replace_labeled_number
}
