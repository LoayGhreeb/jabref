package org.jabref.logic.search.indexing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javafx.util.Pair;

import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.importer.util.FileFieldParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.HeadlessExecutorService;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.search.LuceneIndexer;
import org.jabref.model.search.SearchFieldConstants;
import org.jabref.preferences.PreferencesService;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultLinkedFilesIndexer implements LuceneIndexer {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultLinkedFilesIndexer.class);
    private static final DocumentReader DOCUMENT_READER = new DocumentReader();
    private static int NUMBER_OF_UNSAVED_LIBRARIES = 1;

    private final BibDatabaseContext databaseContext;
    private final TaskExecutor taskExecutor;
    private final PreferencesService preferences;
    private final String libraryName;
    private final Directory indexDirectory;
    private final IndexWriter indexWriter;
    private final SearcherManager searcherManager;
    private Path indexDirectoryPath;
    private Map<String, Long> indexedFiles;
    private IndexSearcher indexSearcher;

    public DefaultLinkedFilesIndexer(BibDatabaseContext databaseContext, TaskExecutor executor, PreferencesService preferences) throws IOException {
        this.databaseContext = databaseContext;
        this.taskExecutor = executor;
        this.preferences = preferences;
        this.libraryName = databaseContext.getDatabasePath().map(path -> path.getFileName().toString()).orElseGet(() -> "untitled");
        this.indexedFiles = new ConcurrentHashMap<>();

        indexDirectoryPath = databaseContext.getFulltextIndexPath();
        IndexWriterConfig config = new IndexWriterConfig(SearchFieldConstants.ANALYZER);
        if (indexDirectoryPath.getFileName().toString().equals("unsaved")) {
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            indexDirectoryPath = indexDirectoryPath.resolveSibling("unsaved" + NUMBER_OF_UNSAVED_LIBRARIES++);
        }

        this.indexDirectory = FSDirectory.open(indexDirectoryPath);
        this.indexWriter = new IndexWriter(indexDirectory, config);
        this.searcherManager = new SearcherManager(indexWriter, null);
    }

    @Override
    public void updateOnStart() {
        indexedFiles = getLinkedFilesFromIndex();
        Map<String, Pair<Long, Path>> currentFiles = getLinkedFilesFromEntries(databaseContext.getEntries());

        Set<String> filesToRemove = new HashSet<>();
        for (Map.Entry<String, Long> entry : indexedFiles.entrySet()) {
            String fileLink = entry.getKey();
            long modification = entry.getValue();
            if (!currentFiles.containsKey(fileLink)) {
                LOGGER.debug("File {} has been removed from the library. Will be removed from the index", fileLink);
                filesToRemove.add(fileLink);
            } else if (currentFiles.get(fileLink).getKey() > modification) {
                LOGGER.debug("File {} has been modified since last indexing. Will be removed from the index.", fileLink);
                filesToRemove.add(fileLink);
            }
        }
        removeFromIndex(filesToRemove);

        Map<String, Pair<Long, Path>> filesToAdd = new HashMap<>();
        for (Map.Entry<String, Pair<Long, Path>> entry : currentFiles.entrySet()) {
            String fileLink = entry.getKey();
            long modification = entry.getValue().getKey();
            if (!indexedFiles.containsKey(fileLink)) {
                LOGGER.debug("File {} has been added to the library. Will be added to the index.", fileLink);
                filesToAdd.put(fileLink, entry.getValue());
            }
        }
        addToIndex(filesToAdd);
    }

    @Override
    public void addToIndex(Collection<BibEntry> entries) {
        addToIndex(getLinkedFilesFromEntries(entries));
    }

    private void addToIndex(Set<LinkedFile> linkedFiles) {
        Map<String, Pair<Long, Path>> filesToAdd = new HashMap<>();
        for (LinkedFile linkedFile : linkedFiles) {
            Pair<Long, Path> fileInfo = getLinkedFileInfo(linkedFile);
            if (fileInfo != null) {
                filesToAdd.put(linkedFile.getLink(), fileInfo);
            }
        }
        addToIndex(filesToAdd);
    }

    private void addToIndex(Map<String, Pair<Long, Path>> linkedFiles) {
        for (String fileLink : linkedFiles.keySet()) {
            if (indexedFiles.containsKey(fileLink)) {
                LOGGER.debug("File {} is already indexed.", fileLink);
                linkedFiles.remove(fileLink);
            }
        }
        if (linkedFiles.isEmpty()) {
            return;
        }

        UiTaskExecutor.runInJavaFXThread(() -> {
            new BackgroundTask<>() {
                @Override
                protected Void call() {
                    int i = 1;
                    LOGGER.debug("Adding {} files to index", linkedFiles.size());
                    for (Map.Entry<String, Pair<Long, Path>> entry : linkedFiles.entrySet()) {
                        if (isCanceled()) {
                            updateMessage(Localization.lang("Indexing canceled: %0 of %1 files added to the index.", i, linkedFiles.size()));
                            break;
                        }
                        addToIndex(entry.getKey(), entry.getValue().getKey(), entry.getValue().getValue());
                        updateProgress(i, linkedFiles.size());
                        updateMessage(Localization.lang("Indexing %0. %1 of %2 files added to the index.", entry.getValue().getValue().getFileName(), i, linkedFiles.size()));
                        i++;
                    }
                    updateMessage(Localization.lang("Indexing completed: %0 files added to the index.", linkedFiles.size()));
                    return null;
                }
            }.willBeRecoveredAutomatically(true)
             .showToUser(true)
             .setTitle(Localization.lang("Indexing pdf files for %0", libraryName))
             .executeWith(taskExecutor);
        });
    }

    private void addToIndex(String fileLink, long modifiedTime, Path resolvedPath) {
        LOGGER.debug("Adding file {} to the index.", fileLink);
        List<Document> pages = DOCUMENT_READER.readPdfContents(fileLink, resolvedPath);
        try {
            indexWriter.addDocuments(pages);
            indexedFiles.put(fileLink, modifiedTime);
        } catch (IOException e) {
            LOGGER.warn("Could not add the document {} to the index.", fileLink, e);
        }
    }

    @Override
    public void removeFromIndex(Collection<BibEntry> entries) {
        Map<String, Pair<Long, Path>> linkedFiles = getLinkedFilesFromEntries(entries);
        removeUnlinkedFiles(entries, linkedFiles.keySet());
    }

    private void removeUnlinkedFiles(Collection<BibEntry> entriesToRemove, Collection<String> linkedFiles) {
        Map<String, Set<BibEntry>> currentFiles = new HashMap<>();
        for (BibEntry entry : databaseContext.getEntries()) {
            for (LinkedFile linkedFile : entry.getFiles()) {
                currentFiles.computeIfAbsent(linkedFile.getLink(), k -> new HashSet<>()).add(entry);
            }
        }

        Set<String> filesToRemove = linkedFiles.stream()
                                               .filter(link -> {
                                                   Set<BibEntry> entriesLinkedToFile = currentFiles.get(link);
                                                   if (entriesLinkedToFile != null) {
                                                       entriesLinkedToFile.removeAll(entriesToRemove);
                                                       return entriesLinkedToFile.isEmpty();
                                                   }
                                                   return true;
                                               })
                                               .collect(Collectors.toSet());
        removeFromIndex(filesToRemove);
    }

    private void removeFromIndex(Set<String> links) {
        links.forEach(this::removeFromIndex);
    }

    private void removeFromIndex(String fileLink) {
        try {
            LOGGER.debug("Removing file {} from index.", fileLink);
            indexWriter.deleteDocuments(new Term(SearchFieldConstants.PATH.toString(), fileLink));
            indexedFiles.remove(fileLink);
        } catch (IOException e) {
            LOGGER.warn("Could not remove linked file {} from index.", fileLink, e);
        }
    }

    @Override
    public void updateEntry(BibEntry entry, String oldValue, String newValue) {
        Set<LinkedFile> oldFiles = new HashSet<>(FileFieldParser.parse(oldValue));
        Set<LinkedFile> newFiles = new HashSet<>(FileFieldParser.parse(newValue));

        Set<LinkedFile> toRemove = new HashSet<>(oldFiles);
        toRemove.removeAll(newFiles);
        removeUnlinkedFiles(List.of(entry), toRemove.stream().map(LinkedFile::getLink).collect(Collectors.toSet()));

        Set<LinkedFile> toAdd = new HashSet<>(newFiles);
        toAdd.removeAll(oldFiles);
        addToIndex(toAdd);
    }

    @Override
    public void removeAllFromIndex() {
        try {
            LOGGER.debug("Removing all linked files from index.");
            indexWriter.deleteAll();
            indexedFiles.clear();
            LOGGER.debug("Removed all linked files");
        } catch (IOException e) {
            LOGGER.error("Error removing all linked files from index", e);
        }
    }

    @Override
    public void rebuildIndex() {
        removeAllFromIndex();
        addToIndex(getLinkedFilesFromEntries(databaseContext.getEntries()));
    }

    private Map<String, Long> getLinkedFilesFromIndex() {
        LOGGER.debug("Getting all linked files from index.");
        Map<String, Long> linkedFiles = new HashMap<>();
        try {
            TermQuery query = new TermQuery(new Term(SearchFieldConstants.PAGE_NUMBER.toString(), "1"));
            IndexSearcher searcher = getIndexSearcher();
            StoredFields storedFields = searcher.storedFields();
            TopDocs allDocs = searcher.search(query, Integer.MAX_VALUE);
            for (ScoreDoc scoreDoc : allDocs.scoreDocs) {
                Document doc = storedFields.document(scoreDoc.doc);
                var pathField = doc.getField(SearchFieldConstants.PATH.toString());
                var modifiedField = doc.getField(SearchFieldConstants.MODIFIED.toString());
                if (pathField != null && modifiedField != null) {
                    linkedFiles.put(pathField.stringValue(), Long.valueOf(modifiedField.stringValue()));
                }
            }
        } catch (IOException e) {
            LOGGER.error("Error getting linked files from index", e);
        }
        return linkedFiles;
    }

    private Map<String, Pair<Long, Path>> getLinkedFilesFromEntries(Collection<BibEntry> entries) {
        Map<String, Pair<Long, Path>> linkedFiles = new HashMap<>();
        for (BibEntry entry : entries) {
            for (LinkedFile linkedFile : entry.getFiles()) {
                Pair<Long, Path> fileInfo = getLinkedFileInfo(linkedFile);
                if (fileInfo != null) {
                    linkedFiles.put(linkedFile.getLink(), fileInfo);
                }
            }
        }
        return linkedFiles;
    }

    private Pair<Long, Path> getLinkedFileInfo(LinkedFile linkedFile) {
        if (linkedFile.isOnlineLink() || !StandardFileType.PDF.getName().equals(linkedFile.getFileType())) {
            LOGGER.debug("Linked file {} is not a local PDF file. The file will not be indexed.", linkedFile.getLink());
            return null;
        }
        Optional<Path> resolvedPath = linkedFile.findIn(databaseContext, preferences.getFilePreferences());
        if (resolvedPath.isEmpty()) {
            LOGGER.debug("Could not resolve path of linked file {}. The file will not be indexed.", linkedFile.getLink());
            return null;
        }
        try {
            long fsModifiedTime = Files.getLastModifiedTime(resolvedPath.get()).to(TimeUnit.SECONDS);
            return new Pair<>(fsModifiedTime, resolvedPath.get());
        } catch (IOException e) {
            LOGGER.warn("Could not check the modification time of file {}.", linkedFile.getLink(), e);
            return null;
        }
    }

    @Override
    public IndexSearcher getIndexSearcher() {
        LOGGER.debug("Getting index searcher for linked files index");
        try {
            if (indexSearcher != null) {
                LOGGER.debug("Releasing index searcher for linked files index");
                searcherManager.release(indexSearcher);
            }
            searcherManager.maybeRefresh();
            indexSearcher = searcherManager.acquire();
        } catch (IOException e) {
            LOGGER.error("Error refreshing searcher", e);
        }
        return indexSearcher;
    }

    private void optimizeIndex() {
        LOGGER.debug("Optimizing index");
        if (indexWriter.hasDeletions()) {
            try {
                LOGGER.debug("Forcing merge deletes");
                indexWriter.forceMergeDeletes(true);
            } catch (IOException e) {
                LOGGER.warn("Could not force merge deletes.", e);
            }
        }
        try {
            LOGGER.debug("Forcing merge segments to 1 segment");
            indexWriter.forceMerge(1, true);
        } catch (IOException e) {
            LOGGER.warn("Could not force merge segments.", e);
        }
    }

    @Override
    public void close() {
        HeadlessExecutorService.INSTANCE.execute(() -> {
            try {
                LOGGER.debug("Closing linked files index");
                searcherManager.close();
                optimizeIndex();
                indexWriter.close();
                indexDirectory.close();
                LOGGER.debug("Linked files index closed");
                if (databaseContext.getFulltextIndexPath().getFileName().toString().equals("unsaved")) {
                    LOGGER.debug("Deleting unsaved index directory");
                    FileUtils.deleteDirectory(indexDirectoryPath.toFile());
                }
            } catch (IOException e) {
                LOGGER.error("Error while closing linked files index", e);
            }
        });
    }
}
