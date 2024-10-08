package org.jabref.logic.importer;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.identifier.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a convenient interface for {@link IdFetcher}, which follow the usual three-step procedure:
 * 1. Open a URL based on the search query
 * 2. Parse the response to get a list of {@link BibEntry}
 * 3. Extract identifier
 */
public interface IdParserFetcher<T extends Identifier> extends IdFetcher<T>, ParserFetcher {

    Logger LOGGER = LoggerFactory.getLogger(IdParserFetcher.class);

    /**
     * Constructs a URL based on the {@link BibEntry}.
     *
     * @param entry the entry to look information for
     */
    URL getURLForEntry(BibEntry entry) throws URISyntaxException, MalformedURLException, FetcherException;

    /**
     * Returns the parser used to convert the response to a list of {@link BibEntry}.
     */
    Parser getParser();

    /**
     * Extracts the identifier from the list of fetched entries.
     *
     * @param inputEntry     the entry for which we are searching the identifier (can be used to find closest match in
     *                       the result)
     * @param fetchedEntries list of entries returned by the web service
     */
    Optional<T> extractIdentifier(BibEntry inputEntry, List<BibEntry> fetchedEntries) throws FetcherException;

    @Override
    default Optional<T> findIdentifier(BibEntry entry) throws FetcherException {
        Objects.requireNonNull(entry);

        URL urlForEntry;
        try {
            urlForEntry = getURLForEntry(entry);
        } catch (URISyntaxException | MalformedURLException e) {
            throw new FetcherException("Search URL is malformed", e);
        }
        try (InputStream stream = new BufferedInputStream(urlForEntry.openStream())) {
            List<BibEntry> fetchedEntries = getParser().parseEntries(stream);

            if (fetchedEntries.isEmpty()) {
                return Optional.empty();
            }

            // Post-cleanup
            fetchedEntries.forEach(this::doPostCleanup);

            return extractIdentifier(entry, fetchedEntries);
        } catch (FileNotFoundException e) {
            LOGGER.debug("Id not found");
            return Optional.empty();
        } catch (IOException e) {
            // check for the case where we already have a FetcherException from UrlDownload
            if (e.getCause() instanceof FetcherException fe) {
                throw fe;
            }
            throw new FetcherException(urlForEntry, "An I/O exception occurred", e);
        } catch (ParseException e) {
            throw new FetcherException(urlForEntry, "An internal parser error occurred", e);
        }
    }
}
