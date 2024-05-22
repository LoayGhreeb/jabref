package org.jabref.logic.pdf.search;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;

import org.jabref.logic.search.indexing.LuceneIndexer;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.preferences.FilePreferences;
import org.jabref.preferences.PreferencesService;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.NIOFSDirectory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LuceneIndexerTest {

    private LuceneIndexer indexer;
    private BibDatabase database;
    private BibDatabaseContext context = mock(BibDatabaseContext.class);

    @BeforeEach
    public void setUp(@TempDir Path indexDir) throws IOException {
        FilePreferences filePreferences = mock(FilePreferences.class);
        when(filePreferences.shouldFulltextIndexLinkedFiles()).thenReturn(true);
        PreferencesService preferencesService = mock(PreferencesService.class);
        when(preferencesService.getFilePreferences()).thenReturn(filePreferences);
        this.database = new BibDatabase();

        this.context = mock(BibDatabaseContext.class);
        when(context.getDatabasePath()).thenReturn(Optional.of(Path.of("src/test/resources/pdfs/")));
        when(context.getFileDirectories(Mockito.any())).thenReturn(Collections.singletonList(Path.of("src/test/resources/pdfs")));
        when(context.getFulltextIndexPath()).thenReturn(indexDir);
        when(context.getDatabase()).thenReturn(database);
        when(context.getEntries()).thenReturn(database.getEntries());
        this.indexer = LuceneIndexer.of(context, preferencesService);
    }

    @Test
    public void exampleThesisIndex() throws IOException {
        // given
        BibEntry entry = new BibEntry(StandardEntryType.PhdThesis);
        entry.setFiles(Collections.singletonList(new LinkedFile("Example Thesis", "thesis-example.pdf", StandardFileType.PDF.getName())));
        database.insertEntry(entry);

        // when
<<<<<<<< HEAD:src/test/java/org/jabref/logic/pdf/search/LuceneIndexerTest.java
        indexer.createIndex();
        for (BibEntry bibEntry : context.getEntries()) {
            indexer.addBibFieldsToIndex(bibEntry);
            indexer.addLinkedFilesToIndex(bibEntry);
        }
========
        indexer.rebuildIndex();
>>>>>>>> main:src/test/java/org/jabref/logic/pdf/search/PdfIndexerTest.java

        // then
        try (IndexReader reader = DirectoryReader.open(new NIOFSDirectory(context.getFulltextIndexPath()))) {
            assertEquals(34, reader.numDocs());
        }
    }

    @Test
    public void doNotIndexNonPdf() throws IOException {
        // given
        BibEntry entry = new BibEntry(StandardEntryType.PhdThesis)
                .withFiles(Collections.singletonList(new LinkedFile("Example Thesis", "thesis-example.aux", StandardFileType.AUX.getName())));
        database.insertEntry(entry);

        // when
<<<<<<<< HEAD:src/test/java/org/jabref/logic/pdf/search/LuceneIndexerTest.java
        indexer.createIndex();
        for (BibEntry bibEntry : context.getEntries()) {
            indexer.addBibFieldsToIndex(bibEntry);
            indexer.addLinkedFilesToIndex(bibEntry);
        }

        // then
        try (IndexReader reader = DirectoryReader.open(new NIOFSDirectory(context.getFulltextIndexPath()))) {
            assertEquals(1, reader.numDocs());
========
        indexer.rebuildIndex();

        // then
        try (IndexReader reader = DirectoryReader.open(indexer.indexWriter)) {
            assertEquals(0, reader.numDocs());
>>>>>>>> main:src/test/java/org/jabref/logic/pdf/search/PdfIndexerTest.java
        }
    }

    @Test
    public void dontIndexOnlineLinks() throws IOException {
        // given
        BibEntry entry = new BibEntry(StandardEntryType.PhdThesis);
        entry.setFiles(Collections.singletonList(new LinkedFile("Example Thesis", "https://raw.githubusercontent.com/JabRef/jabref/main/src/test/resources/pdfs/thesis-example.pdf", StandardFileType.PDF.getName())));
        database.insertEntry(entry);

        // when
<<<<<<<< HEAD:src/test/java/org/jabref/logic/pdf/search/LuceneIndexerTest.java
        indexer.createIndex();
        for (BibEntry bibEntry : context.getEntries()) {
            indexer.addBibFieldsToIndex(bibEntry);
            indexer.addLinkedFilesToIndex(bibEntry);
        }

        // then
        try (IndexReader reader = DirectoryReader.open(new NIOFSDirectory(context.getFulltextIndexPath()))) {
            assertEquals(1, reader.numDocs());
========
        indexer.rebuildIndex();

        // then
        try (IndexReader reader = DirectoryReader.open(indexer.indexWriter)) {
            assertEquals(0, reader.numDocs());
>>>>>>>> main:src/test/java/org/jabref/logic/pdf/search/PdfIndexerTest.java
        }
    }

    @Test
    public void exampleThesisIndexWithKey() throws IOException {
        // given
        BibEntry entry = new BibEntry(StandardEntryType.PhdThesis);
        entry.setCitationKey("Example2017");
        entry.setFiles(Collections.singletonList(new LinkedFile("Example Thesis", "thesis-example.pdf", StandardFileType.PDF.getName())));
        database.insertEntry(entry);

        // when
<<<<<<<< HEAD:src/test/java/org/jabref/logic/pdf/search/LuceneIndexerTest.java
        indexer.createIndex();
        for (BibEntry bibEntry : context.getEntries()) {
            indexer.addBibFieldsToIndex(bibEntry);
            indexer.addLinkedFilesToIndex(bibEntry);
        }
========
        indexer.rebuildIndex();
>>>>>>>> main:src/test/java/org/jabref/logic/pdf/search/PdfIndexerTest.java

        // then
        try (IndexReader reader = DirectoryReader.open(new NIOFSDirectory(context.getFulltextIndexPath()))) {
            assertEquals(34, reader.numDocs());
        }
    }

    @Test
    public void metaDataIndex() throws IOException {
        // given
        BibEntry entry = new BibEntry(StandardEntryType.Article);
        entry.setFiles(Collections.singletonList(new LinkedFile("Example Thesis", "metaData.pdf", StandardFileType.PDF.getName())));

        database.insertEntry(entry);

        // when
<<<<<<<< HEAD:src/test/java/org/jabref/logic/pdf/search/LuceneIndexerTest.java
        indexer.createIndex();
        for (BibEntry bibEntry : context.getEntries()) {
            indexer.addBibFieldsToIndex(bibEntry);
            indexer.addLinkedFilesToIndex(bibEntry);
        }
========
        indexer.rebuildIndex();
>>>>>>>> main:src/test/java/org/jabref/logic/pdf/search/PdfIndexerTest.java

        // then
        try (IndexReader reader = DirectoryReader.open(new NIOFSDirectory(context.getFulltextIndexPath()))) {
            assertEquals(2, reader.numDocs());
        }
    }

    @Test
<<<<<<<< HEAD:src/test/java/org/jabref/logic/pdf/search/LuceneIndexerTest.java
    public void testFlushIndex() throws IOException {
        // given
        BibEntry entry = new BibEntry(StandardEntryType.PhdThesis);
        entry.setCitationKey("Example2017");
        entry.setFiles(Collections.singletonList(new LinkedFile("Example Thesis", "thesis-example.pdf", StandardFileType.PDF.getName())));
        database.insertEntry(entry);

        indexer.createIndex();
        for (BibEntry bibEntry : context.getEntries()) {
            indexer.addBibFieldsToIndex(bibEntry);
            indexer.addLinkedFilesToIndex(bibEntry);
        }
        // index actually exists
        try (IndexReader reader = DirectoryReader.open(new NIOFSDirectory(context.getFulltextIndexPath()))) {
            assertEquals(34, reader.numDocs());
        }

        // when
        indexer.flushIndex();

        // then
        try (IndexReader reader = DirectoryReader.open(new NIOFSDirectory(context.getFulltextIndexPath()))) {
            assertEquals(0, reader.numDocs());
        }
    }

    @Test
========
>>>>>>>> main:src/test/java/org/jabref/logic/pdf/search/PdfIndexerTest.java
    public void exampleThesisIndexAppendMetaData() throws IOException {
        // given
        BibEntry exampleThesis = new BibEntry(StandardEntryType.PhdThesis);
        exampleThesis.setCitationKey("ExampleThesis2017");
        exampleThesis.setFiles(Collections.singletonList(new LinkedFile("Example Thesis", "thesis-example.pdf", StandardFileType.PDF.getName())));
        database.insertEntry(exampleThesis);
<<<<<<<< HEAD:src/test/java/org/jabref/logic/pdf/search/LuceneIndexerTest.java
        indexer.createIndex();
        for (BibEntry bibEntry : context.getEntries()) {
            indexer.addBibFieldsToIndex(bibEntry);
            indexer.addLinkedFilesToIndex(bibEntry);
        }
========

        indexer.rebuildIndex();
>>>>>>>> main:src/test/java/org/jabref/logic/pdf/search/PdfIndexerTest.java

        // index with first entry
        try (IndexReader reader = DirectoryReader.open(new NIOFSDirectory(context.getFulltextIndexPath()))) {
            assertEquals(34, reader.numDocs());
        }

        BibEntry metadata = new BibEntry(StandardEntryType.Article);
        metadata.setCitationKey("MetaData2017");
        metadata.setFiles(Collections.singletonList(new LinkedFile("Metadata file", "metaData.pdf", StandardFileType.PDF.getName())));

        // when
<<<<<<<< HEAD:src/test/java/org/jabref/logic/pdf/search/LuceneIndexerTest.java
        indexer.addBibFieldsToIndex(metadata);
        indexer.addLinkedFilesToIndex(metadata);
========
        indexer.addToIndex(metadata);
>>>>>>>> main:src/test/java/org/jabref/logic/pdf/search/PdfIndexerTest.java

        // then
        try (IndexReader reader = DirectoryReader.open(new NIOFSDirectory(context.getFulltextIndexPath()))) {
            assertEquals(36, reader.numDocs());
        }
    }
}
