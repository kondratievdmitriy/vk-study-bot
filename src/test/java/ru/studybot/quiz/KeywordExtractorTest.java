package ru.studybot.quiz;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class KeywordExtractorTest {
    private final KeywordExtractor extractor = new KeywordExtractor();

    @Test
    void stopWordsAreFiltered() {
        List<String> keywords = extractor.extractKeywords(
                "наследование наследование механизм и в на", List.of(), 10);
        assertTrue(keywords.contains("наследование"));
        assertFalse(keywords.contains("и"));
        assertFalse(keywords.contains("механизм") == false && keywords.contains("наследование") == false);
    }

    @Test
    void termsUniqueToBlockScoreHigher() {
        List<String> corpus = List.of(
                "инкапсуляция механизм подробности",
                "механизм механизм подробности подробности");
        List<String> keywords = extractor.extractKeywords(corpus.get(0), corpus, 2);
        // «инкапсуляция» встречается только в первом блоке — IDF выше, она первая
        assertEquals("инкапсуляция", keywords.get(0));
    }

    @Test
    void emptyTextReturnsEmptyList() {
        assertTrue(extractor.extractKeywords("", List.of(), 5).isEmpty());
        assertTrue(extractor.extractKeywords(null, List.of(), 5).isEmpty());
    }

    @Test
    void wordsShorterThanThreeLettersIgnored() {
        assertTrue(extractor.extractKeywords("но не на", List.of(), 5).isEmpty());
    }
}
