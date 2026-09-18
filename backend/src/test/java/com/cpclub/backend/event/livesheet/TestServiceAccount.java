package com.cpclub.backend.event.livesheet;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * A throwaway service-account key, generated per test run.
 *
 * <p>Generated rather than checked in: a real-looking private key in the
 * repository gets flagged by secret scanners, and a generated one proves the
 * parser handles whatever Google issues rather than one fixed string.</p>
 */
final class TestServiceAccount {

    static final String EMAIL = "attendance@cpclub-test.iam.gserviceaccount.com";
    static final String TOKEN_URI = "https://oauth2.googleapis.com/token";

    final KeyPair keyPair;
    final String pem;

    TestServiceAccount() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            keyPair = generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        String body = Base64.getMimeEncoder(64, "\n".getBytes())
                .encodeToString(keyPair.getPrivate().getEncoded());
        pem = "-----BEGIN PRIVATE KEY-----\n" + body + "\n-----END PRIVATE KEY-----\n";
    }

    /** The key file as Google's console downloads it, trimmed to the fields that matter. */
    String json() {
        return "{"
                + "\"type\":\"service_account\","
                + "\"client_email\":\"" + EMAIL + "\","
                + "\"private_key\":\"" + pem.replace("\n", "\\n") + "\","
                + "\"token_uri\":\"" + TOKEN_URI + "\""
                + "}";
    }
}
