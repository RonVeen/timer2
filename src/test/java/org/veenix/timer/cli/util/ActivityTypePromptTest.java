package org.veenix.timer.cli.util;

import org.junit.jupiter.api.Test;
import org.veenix.timer.model.ActivityType;

import static org.junit.jupiter.api.Assertions.*;

class ActivityTypePromptTest {

    @Test
    void testFormatFirstLetter() {
        String result = ActivityTypePrompt.formatFirstLetter("BUG");
        assertEquals(AnsiColors.BOLD_RED + "B" + AnsiColors.RESET + "UG", result);
    }

    @Test
    void testFormatFirstLetterEmptyString() {
        String result = ActivityTypePrompt.formatFirstLetter("");
        assertEquals("", result);
    }

    @Test
    void testFormatFirstLetterNull() {
        String result = ActivityTypePrompt.formatFirstLetter(null);
        assertNull(result);
    }

    @Test
    void testParseInputEmptyReturnsDefault() {
        ActivityType[] types = ActivityType.values();
        ActivityType result = ActivityTypePrompt.parseInput("", types, ActivityType.DEVELOP);
        assertEquals(ActivityType.DEVELOP, result);
    }

    @Test
    void testParseInputValidNumber() {
        ActivityType[] types = ActivityType.values();
        // 1 should map to BUG (first item)
        ActivityType result = ActivityTypePrompt.parseInput("1", types, ActivityType.DEVELOP);
        assertEquals(ActivityType.BUG, result);
    }

    @Test
    void testParseInputValidNumberLast() {
        ActivityType[] types = ActivityType.values();
        // 8 should map to SUPPORT (last item)
        ActivityType result = ActivityTypePrompt.parseInput("8", types, ActivityType.DEVELOP);
        assertEquals(ActivityType.SUPPORT, result);
    }

    @Test
    void testParseInputInvalidNumberZero() {
        ActivityType[] types = ActivityType.values();
        ActivityType result = ActivityTypePrompt.parseInput("0", types, ActivityType.DEVELOP);
        assertNull(result);
    }

    @Test
    void testParseInputInvalidNumberNine() {
        ActivityType[] types = ActivityType.values();
        ActivityType result = ActivityTypePrompt.parseInput("9", types, ActivityType.DEVELOP);
        assertNull(result);
    }

    @Test
    void testParseInputInvalidNumberNegative() {
        ActivityType[] types = ActivityType.values();
        ActivityType result = ActivityTypePrompt.parseInput("-1", types, ActivityType.DEVELOP);
        assertNull(result);
    }

    @Test
    void testParseInputSingleLetterUppercase() {
        ActivityType[] types = ActivityType.values();
        ActivityType result = ActivityTypePrompt.parseInput("B", types, ActivityType.DEVELOP);
        assertEquals(ActivityType.BUG, result);
    }

    @Test
    void testParseInputSingleLetterLowercase() {
        ActivityType[] types = ActivityType.values();
        ActivityType result = ActivityTypePrompt.parseInput("b", types, ActivityType.DEVELOP);
        assertEquals(ActivityType.BUG, result);
    }

    @Test
    void testParseInputSingleLetterD() {
        ActivityType[] types = ActivityType.values();
        ActivityType result = ActivityTypePrompt.parseInput("D", types, ActivityType.DEVELOP);
        assertEquals(ActivityType.DEVELOP, result);
    }

    @Test
    void testParseInputSingleLetterG() {
        ActivityType[] types = ActivityType.values();
        ActivityType result = ActivityTypePrompt.parseInput("G", types, ActivityType.DEVELOP);
        assertEquals(ActivityType.GENERAL, result);
    }

    @Test
    void testParseInputSingleLetterI() {
        ActivityType[] types = ActivityType.values();
        ActivityType result = ActivityTypePrompt.parseInput("I", types, ActivityType.DEVELOP);
        assertEquals(ActivityType.INFRA, result);
    }

    @Test
    void testParseInputSingleLetterM() {
        ActivityType[] types = ActivityType.values();
        ActivityType result = ActivityTypePrompt.parseInput("M", types, ActivityType.DEVELOP);
        assertEquals(ActivityType.MEETING, result);
    }

    @Test
    void testParseInputSingleLetterO() {
        ActivityType[] types = ActivityType.values();
        ActivityType result = ActivityTypePrompt.parseInput("O", types, ActivityType.DEVELOP);
        assertEquals(ActivityType.OUT_OF_OFFICE, result);
    }

    @Test
    void testParseInputSingleLetterP() {
        ActivityType[] types = ActivityType.values();
        ActivityType result = ActivityTypePrompt.parseInput("P", types, ActivityType.DEVELOP);
        assertEquals(ActivityType.PROBLEM, result);
    }

    @Test
    void testParseInputSingleLetterS() {
        ActivityType[] types = ActivityType.values();
        ActivityType result = ActivityTypePrompt.parseInput("S", types, ActivityType.DEVELOP);
        assertEquals(ActivityType.SUPPORT, result);
    }

    @Test
    void testParseInputInvalidLetter() {
        ActivityType[] types = ActivityType.values();
        ActivityType result = ActivityTypePrompt.parseInput("X", types, ActivityType.DEVELOP);
        assertNull(result);
    }

    @Test
    void testParseInputFullNameUppercase() {
        ActivityType[] types = ActivityType.values();
        ActivityType result = ActivityTypePrompt.parseInput("BUG", types, ActivityType.DEVELOP);
        assertEquals(ActivityType.BUG, result);
    }

    @Test
    void testParseInputFullNameLowercase() {
        ActivityType[] types = ActivityType.values();
        ActivityType result = ActivityTypePrompt.parseInput("bug", types, ActivityType.DEVELOP);
        assertEquals(ActivityType.BUG, result);
    }

    @Test
    void testParseInputFullNameMixedCase() {
        ActivityType[] types = ActivityType.values();
        ActivityType result = ActivityTypePrompt.parseInput("BuG", types, ActivityType.DEVELOP);
        assertEquals(ActivityType.BUG, result);
    }

    @Test
    void testParseInputFullNameWithUnderscore() {
        ActivityType[] types = ActivityType.values();
        ActivityType result = ActivityTypePrompt.parseInput("OUT_OF_OFFICE", types, ActivityType.DEVELOP);
        assertEquals(ActivityType.OUT_OF_OFFICE, result);
    }

    @Test
    void testParseInputFullNameWithUnderscoreLowercase() {
        ActivityType[] types = ActivityType.values();
        ActivityType result = ActivityTypePrompt.parseInput("out_of_office", types, ActivityType.DEVELOP);
        assertEquals(ActivityType.OUT_OF_OFFICE, result);
    }

    @Test
    void testParseInputInvalidName() {
        ActivityType[] types = ActivityType.values();
        ActivityType result = ActivityTypePrompt.parseInput("INVALID", types, ActivityType.DEVELOP);
        assertNull(result);
    }
}
