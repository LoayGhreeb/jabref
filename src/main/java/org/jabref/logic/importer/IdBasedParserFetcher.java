package org.jabref.logic.importer;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.strings.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a convenient interface for ID-based fetcher, which follow the usual three-step procedure:
 * 1. Open a URL based on the search query
 * 2. Parse the response to get a list of {@link BibEntry}
 * 3. Post-process fetched entries
 */
public interface IdBasedParserFetcher extends IdBasedFetcher, ParserFetcher {

    Logger LOGGER = LoggerFactory.getLogger(IdBasedParserFetcher.class);

    /**
     * Constructs a URL based on the query.
     *
     * @param identifier the ID
     */
    URL getUrlForIdentifier(String identifier) throws URISyntaxException, MalformedURLException, FetcherException;

    /**
     * Returns the parser used to convert the response to a list of {@link BibEntry}.
     */
    Parser getParser();

    @Override
    default Optional<BibEntry> performSearchById(String identifier) throws FetcherException {
        if (StringUtil.isBlank(identifier)) {
            return Optional.empty();
        }

        try (InputStream stream = getUrlDownload(getUrlForIdentifier(identifier)).asInputStream()) {
            List<BibEntry> fetchedEntries = getParser().parseEntries(stream);

            if (fetchedEntries.isEmpty()) {
                return Optional.empty();
            }

            if (fetchedEntries.size() > 1) {
                LOGGER.info("Fetcher {} found more than one result for identifier {}. We will use the first entry.", getName(), identifier);
            }

            BibEntry entry = fetchedEntries.getFirst();

            // Post-cleanup
            doPostCleanup(entry);

            return Optional.of(entry);
        } catch (URISyntaxException e) {
            throw new FetcherException("Search URI is malformed", e);
        } catch (IOException e) {
            // check for the case where we already have a FetcherException from UrlDownload
            if (e.getCause() instanceof FetcherException fe) {
                throw fe;
            }
            throw new FetcherException("A network error occurred", e);
        } catch (ParseException e) {
            throw new FetcherException("An internal parser error occurred", e);
        }
    }
}
