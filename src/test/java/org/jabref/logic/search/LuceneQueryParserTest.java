package org.jabref.logic.search;

import java.util.stream.Stream;

import org.jabref.model.search.SearchFieldConstants;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LuceneQueryParserTest {

    public static Stream<Arguments> searchQuires() {
        return Stream.of(
                // unicode
                Arguments.of("all:preissinger", "preißinger"),
                Arguments.of("all:jesus", "jesús"),
                Arguments.of("all:breitenbucher", "breitenbücher"),

                // latex
                Arguments.of("all:preissinger", "\"prei{\\\\ss}inger\""),
                Arguments.of("all:jesus", "\"jes{\\\\'{u}}s\""),
                Arguments.of("all:breitenbucher", "\"breitenb{\\\\\\\"{u}}cher\""),

                Arguments.of("groups:/exclude", "groups:\\/exclude")
        );
    }

    @ParameterizedTest
    @MethodSource
    void searchQuires(String expected, String query) throws ParseException {
        QueryParser parser = new QueryParser(SearchFieldConstants.DEFAULT_FIELD.toString(), SearchFieldConstants.LATEX_AWARE_ANALYZER);
        String result = parser.parse(query).toString();
        assertEquals(expected, result);
    }
}
