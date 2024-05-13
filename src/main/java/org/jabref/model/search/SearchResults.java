package org.jabref.model.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public final class SearchResults {

    private final List<SearchResult> searchResults = new LinkedList<>();

    public List<SearchResult> getSortedByScore() {
        List<SearchResult> sortedList = new ArrayList<>(searchResults);
        sortedList.sort((searchResult, t1) -> Float.compare(searchResult.getLuceneScore(), t1.getLuceneScore()));
        return Collections.unmodifiableList(sortedList);
    }

    public List<SearchResult> getSearchResults() {
        return this.searchResults;
    }

    public HashMap<String, List<SearchResult>> getSearchResultsByPath() {
        HashMap<String, List<SearchResult>> resultsByPath = new HashMap<>();
        for (SearchResult result : searchResults) {
            resultsByPath.computeIfAbsent(result.getFilePath(), key -> new ArrayList<>()).add(result);
        }
        return resultsByPath;
    }

    public int numSearchResults() {
        return this.searchResults.size();
    }

    public void addResult(SearchResult result) {
        this.searchResults.add(result);
    }

    public float getMaxSearchScore() {
        return this.searchResults.stream()
                                 .map(SearchResult::getLuceneScore)
                                 .max(Comparator.comparing(Float::floatValue))
                                 .orElse(0F);
    }

    public boolean hasFulltextResults() {
        return this.searchResults.stream().anyMatch(SearchResult::hasFulltextResults);
    }
}
