package org.jabref.logic.importer.util;

import java.io.ByteArrayInputStream;

import org.jabref.logic.importer.ParseException;

import kong.unirest.core.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonReaderTest {

    @Test
    void nullStreamThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> {
            JsonReader.toJsonObject(null);
        });
    }

    @Test
    void invalidJsonThrowsParserException() {
        assertThrows(ParseException.class, () -> {
            JsonReader.toJsonObject(new ByteArrayInputStream("invalid JSON".getBytes()));
        });
    }

    @Test
    void emptyStringResultsInEmptyObject() throws Exception {
        JSONObject result = JsonReader.toJsonObject(new ByteArrayInputStream("".getBytes()));
        assertEquals("{}", result.toString());
    }

    @Test
    void arrayThrowsParserException() {
        // Reason: We expect a JSON object, not a JSON array
        assertThrows(ParseException.class, () -> {
            JsonReader.toJsonObject(new ByteArrayInputStream("[]".getBytes()));
        });
    }

    @Test
    void exampleJsonResultsInSameJson() throws Exception {
        String input = "{\"name\":\"test\"}";
        JSONObject result = JsonReader.toJsonObject(new ByteArrayInputStream(input.getBytes()));
        assertEquals(input, result.toString());
    }
}
