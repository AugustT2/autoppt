package com.example.pptrefresh.naming;

import com.example.pptrefresh.exception.RefreshException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductNameResolverTest {

    @Test
    void extractSegment2Title_ok() {
        assertEquals(
                "中欧瑾添",
                ProductNameResolver.extractSegment2Title(
                        "偏债混", "偏债混-中欧瑾添\n其它"));
    }

    @Test
    void extractSegment2Title_notFound() {
        assertThrows(RefreshException.class, () -> ProductNameResolver.extractSegment2Title("偏债混", "只有别的"));
    }
}
