package com.cpclub.backend.event.livesheet;

import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Map;

/**
 * The parts of a Google service-account JSON key this application uses.
 *
 * <p>A service account is a Google account that belongs to the Cloud project
 * rather than to a person. The live attendance sheet is shared with it, and the
 * server signs in as it to write rows. Only three fields of the downloaded key
 * matter: who it is, the private key it signs with, and where to exchange the
 * signature for an access token.</p>
 *
 * @param clientEmail the service account's address, which the sheet is shared with
 * @param privateKey the RSA key that signs token requests
 * @param tokenUri Google's token endpoint, as the key file names it
 */
public record ServiceAccountKey(String clientEmail, PrivateKey privateKey, String tokenUri) {

    static final String DEFAULT_TOKEN_URI = "https://oauth2.googleapis.com/token";

    /**
     * Reads the key as it is pasted into the environment.
     *
     * <p>Accepts the JSON file's contents as they are, or Base64 of them. Some
     * dashboards mangle multi-line values, and Base64 is the usual way round
     * that, so both are taken rather than documenting one and failing on the
     * other.</p>
     *
     * @param raw the environment value
     * @param objectMapper JSON reader
     * @return the parsed key
     * @throws IllegalArgumentException if the value is not a usable service-account key
     */
    public static ServiceAccountKey parse(String raw, ObjectMapper objectMapper) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("The service-account key is empty.");
        }

        String json = raw.trim();
        if (!json.startsWith("{")) {
            try {
                json = new String(Base64.getDecoder().decode(json), StandardCharsets.UTF_8).trim();
            } catch (IllegalArgumentException notBase64) {
                throw new IllegalArgumentException(
                        "The service-account key is neither JSON nor Base64-encoded JSON.");
            }
        }

        Map<?, ?> fields;
        try {
            fields = objectMapper.readValue(json, Map.class);
        } catch (RuntimeException badJson) {
            throw new IllegalArgumentException("The service-account key is not valid JSON.");
        }

        String clientEmail = requireText(fields, "client_email");
        String privateKeyPem = requireText(fields, "private_key");
        Object tokenUri = fields.get("token_uri");

        return new ServiceAccountKey(
                clientEmail,
                readPrivateKey(privateKeyPem),
                tokenUri instanceof String uri && !uri.isBlank() ? uri : DEFAULT_TOKEN_URI
        );
    }

    /**
     * Decodes a PKCS#8 PEM private key, which is the form Google issues.
     *
     * <p>Literal {@code \n} sequences are removed as well as real line breaks: a
     * key copied out of the JSON by hand, rather than the whole file, arrives with
     * the escapes still in it.</p>
     */
    static PrivateKey readPrivateKey(String pem) {
        String base64 = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("\\n", "")
                .replaceAll("\\s", "");
        try {
            byte[] der = Base64.getDecoder().decode(base64);
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (IllegalArgumentException | GeneralSecurityException e) {
            throw new IllegalArgumentException("The service-account private_key could not be read.");
        }
    }

    private static String requireText(Map<?, ?> fields, String name) {
        Object value = fields.get(name);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException("The service-account key has no " + name + ".");
        }
        return text;
    }

    /** Never print the private key, even by accident in a log line. */
    @Override
    public String toString() {
        return "ServiceAccountKey[clientEmail=" + clientEmail + "]";
    }
}
