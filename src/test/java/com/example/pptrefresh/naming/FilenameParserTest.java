package com.example.pptrefresh.naming;

import com.example.pptrefresh.exception.RefreshException;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FilenameParserTest {

    @Test
    void parsesValidName() {
        ParsedFilename p = FilenameParser.parse(Path.of("20260430-混合固收+-M1.pptx"));
        assertEquals("20260430", p.prefix());
        assertEquals("混合固收+", p.segment2());
        assertEquals("M1", p.segment3());
        assertEquals("混合固收+-M1", p.deckType());
    }

    @Test
    void rejectsWrongSegmentCount() {
        assertThrows(RefreshException.class, () -> FilenameParser.parse(Path.of("a-b.pptx")));
    }
}
