package org.jabref.logic.search;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import javafx.beans.property.BooleanProperty;

import org.jabref.gui.util.CurrentThreadTaskExecutor;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.search.SearchFlags;
import org.jabref.model.search.SearchQuery;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.preferences.FilePreferences;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DatabaseSearcherWithBibFilesTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseSearcherWithBibFilesTest.class);
    private static final TaskExecutor TASK_EXECUTOR = new CurrentThreadTaskExecutor();
    private static final String TITLE_SENTENCE_CASED = "title-sentence-cased";
    private static final String TITLE_MIXED_CASED = "title-mixed-cased";
    private static final String TITLE_UPPER_CASED = "title-upper-cased";
    private static final String MINIMAL_SENTENCE_CASE = "minimal-sentence-case";
    private static final String MINIMAL_ALL_UPPER_CASE = "minimal-all-upper-case";
    private static final String MINIMAL_MIXED_CASE = "minimal-mixed-case";
    private static final String MINIMAL_NOTE_SENTENCE_CASE = "minimal-note-sentence-case";
    private static final String MINIMAL_NOTE_ALL_UPPER_CASE = "minimal-note-all-upper-case";
    private static final String MINIMAL_NOTE_MIXED_CASE = "minimal-note-mixed-case";

    private static final FilePreferences FILE_PREFERENCES = mock(FilePreferences.class);
    @TempDir
    private Path indexDir;

    private BibDatabaseContext initializeDatabaseFromPath(String testFile) throws Exception {
        return initializeDatabaseFromPath(Path.of(Objects.requireNonNull(DatabaseSearcherWithBibFilesTest.class.getResource(testFile)).toURI()));
    }

    private BibDatabaseContext initializeDatabaseFromPath(Path testFile) throws Exception {
        ParserResult result = new BibtexImporter(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS), new DummyFileUpdateMonitor()).importDatabase(testFile);
        BibDatabaseContext databaseContext = result.getDatabaseContext();

        when(FILE_PREFERENCES.shouldFulltextIndexLinkedFiles()).thenReturn(true);
        when(FILE_PREFERENCES.fulltextIndexLinkedFilesProperty()).thenReturn(mock(BooleanProperty.class));
        return databaseContext;
    }

    private static Stream<Arguments> searchLibrary() {
        return Stream.of(
                // empty library
                Arguments.of(List.of(), "empty.bib", "Test", EnumSet.noneOf(SearchFlags.class)),

                // test-library-title-casing
                Arguments.of(List.of(), "test-library-title-casing.bib", "NotExisting", EnumSet.noneOf(SearchFlags.class)),
                Arguments.of(List.of(TITLE_SENTENCE_CASED, TITLE_MIXED_CASED, TITLE_UPPER_CASED), "test-library-title-casing.bib", "Title", EnumSet.noneOf(SearchFlags.class)),

                Arguments.of(List.of(), "test-library-title-casing.bib", "title:NotExisting", EnumSet.noneOf(SearchFlags.class)),
                Arguments.of(List.of(TITLE_SENTENCE_CASED, TITLE_MIXED_CASED, TITLE_UPPER_CASED), "test-library-title-casing.bib", "title:Title", EnumSet.noneOf(SearchFlags.class)),

                // Arguments.of(List.of(), "test-library-title-casing.bib", "title:TiTLE", EnumSet.of(SearchFlags.CASE_SENSITIVE)),
                // Arguments.of(List.of(titleSentenceCased), "test-library-title-casing.bib", "title:Title", EnumSet.of(SearchFlags.CASE_SENSITIVE)),

                // Arguments.of(List.of(), "test-library-title-casing.bib", "TiTLE", EnumSet.of(SearchFlags.CASE_SENSITIVE)),
                // Arguments.of(List.of(titleMixedCased), "test-library-title-casing.bib", "TiTle", EnumSet.of(SearchFlags.CASE_SENSITIVE)),

                // Arguments.of(List.of(), "test-library-title-casing.bib", "title:NotExisting", EnumSet.of(SearchFlags.CASE_SENSITIVE)),
                // Arguments.of(List.of(titleMixedCased), "test-library-title-casing.bib", "title:TiTle", EnumSet.of(SearchFlags.CASE_SENSITIVE)),

                Arguments.of(List.of(), "test-library-title-casing.bib", "/[Y]/", EnumSet.noneOf(SearchFlags.class)),

                // test-library-with-attached-files
                Arguments.of(List.of(), "test-library-with-attached-files.bib", "NotExisting.", EnumSet.of(SearchFlags.FULLTEXT)),
                Arguments.of(List.of(MINIMAL_SENTENCE_CASE, MINIMAL_ALL_UPPER_CASE, MINIMAL_MIXED_CASE), "test-library-with-attached-files.bib", "This is a short sentence, comma included.", EnumSet.of(SearchFlags.FULLTEXT)),
                Arguments.of(List.of(MINIMAL_SENTENCE_CASE, MINIMAL_ALL_UPPER_CASE, MINIMAL_MIXED_CASE), "test-library-with-attached-files.bib", "comma", EnumSet.of(SearchFlags.FULLTEXT)),

                // TODO: PDF search does not support case sensitive search (yet)
                // Arguments.of(List.of(minimalAllUpperCase, minimalMixedCase), "test-library-with-attached-files.bib", "THIS", EnumSet.of(SearchFlags.FULLTEXT, SearchFlags.CASE_SENSITIVE)),
                // Arguments.of(List.of(minimalAllUpperCase), "test-library-with-attached-files.bib", "THIS is a short sentence, comma included.", EnumSet.of(SearchFlags.FULLTEXT, SearchFlags.CASE_SENSITIVE)),
                // Arguments.of(List.of(minimalSentenceCase, minimalAllUpperCase, minimalMixedCase), "test-library-with-attached-files.bib", "comma", EnumSet.of(SearchFlags.FULLTEXT, SearchFlags.CASE_SENSITIVE)),
                // Arguments.of(List.of(minimalNoteAllUpperCase), "test-library-with-attached-files.bib", "THIS IS A SHORT SENTENCE, COMMA INCLUDED.", EnumSet.of(SearchFlags.FULLTEXT, SearchFlags.CASE_SENSITIVE)),

                Arguments.of(List.of(), "test-library-with-attached-files.bib", "NotExisting", EnumSet.of(SearchFlags.FULLTEXT)),
                Arguments.of(List.of(MINIMAL_NOTE_SENTENCE_CASE, MINIMAL_NOTE_ALL_UPPER_CASE, MINIMAL_NOTE_MIXED_CASE), "test-library-with-attached-files.bib", "world", EnumSet.of(SearchFlags.FULLTEXT)),
                Arguments.of(List.of(MINIMAL_NOTE_SENTENCE_CASE, MINIMAL_NOTE_ALL_UPPER_CASE, MINIMAL_NOTE_MIXED_CASE), "test-library-with-attached-files.bib", "Hello World", EnumSet.of(SearchFlags.FULLTEXT))

                // TODO: PDF search does not support case sensitive search (yet)
                // Arguments.of(List.of(minimalNoteAllUpperCase), "test-library-with-attached-files.bib", "HELLO WORLD", EnumSet.of(SearchFlags.FULLTEXT, SearchFlags.CASE_SENSITIVE)),
                // Arguments.of(List.of(), "test-library-with-attached-files.bib", "NotExisting", EnumSet.of(SearchFlags.FULLTEXT, SearchFlags.CASE_SENSITIVE))
        );
    }

    @ParameterizedTest
    @MethodSource
    void searchLibrary(List<String> expected, String testFile, String query, EnumSet<SearchFlags> searchFlags) throws Exception {
        BibDatabaseContext databaseContext = initializeDatabaseFromPath(testFile);
        List<String> matches = new DatabaseSearcher(new SearchQuery(query, searchFlags), databaseContext, TASK_EXECUTOR, FILE_PREFERENCES)
                .getMatches().stream()
                .map(BibEntry::getCitationKey)
                .flatMap(Optional::stream)
                .toList();

        LOGGER.trace("Expected: {}", expected.stream().sorted().toList());
        LOGGER.trace("Matches: {}", matches.stream().sorted().toList());
        assertThat(expected, Matchers.containsInAnyOrder(matches.toArray()));
    }
}
