package net.unit8.javamonster.relay;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.relay.Relay;

import java.io.IOException;
import java.util.*;

public class ObjectCursor {
    private static final Relay relay = new Relay();
    private static final java.util.Base64.Encoder encoder = java.util.Base64.getUrlEncoder().withoutPadding();
    private static final java.util.Base64.Decoder decoder = java.util.Base64.getUrlDecoder();
    private static final ObjectMapper mapper = new ObjectMapper();

    private Map<String, Object> properties;

    private ObjectCursor(Map<String, Object> properties) {
        this.properties = properties;
    }

    public static final ObjectCursor INVALID = new ObjectCursor(Collections.emptyMap());

    public static ObjectCursor fromEncoded(String cursor) {
        try {
            return new ObjectCursor(mapper.readValue(
                    decoder.decode(cursor),
                    new TypeReference<>() {
                    }));
        } catch (IOException e) {
            throw new IllegalArgumentException(cursor + " is wrong format.");
        }
    }

    public Set<String> keySet() {
        return properties.keySet();
    }

    public Object get(String key) {
        return properties.get(key);
    }

    public void validate(List<String> expectedKeys) {
        Set<String> actualKeySet = properties.keySet();
        Set<String> expectedKeySet = new HashSet<>(expectedKeys);

        for (String key : actualKeySet) {
            if (!expectedKeySet.contains(key)) {
                throw new IllegalArgumentException("Invalid cursor. The column '"
                        + key + "' is not in the sort key");
            }
        }

        for (String key : expectedKeys) {
            if (!actualKeySet.contains(key)) {
                throw new IllegalArgumentException("Invalid cursor. The column '"
                        + key + "' is not in the cursor");
            }
        }
    }
}
