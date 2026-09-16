/**
 * Auth Validation Schemas
 *
 * Empty on purpose, and kept as a file rather than deleted so the reason is
 * recorded where the next person looks for it.
 *
 * Sign-in is a Google ID token exchanged at POST /api/auth/google. There is no
 * email field, no password field and no registration form, so there is nothing
 * left for the browser to validate before submitting: the only credential is
 * one the browser never composes and cannot alter, and every check that
 * matters -- the signature, the audience, the @dau.ac.in domain -- happens on
 * the server.
 *
 * The welcome step at /welcome does validate its one required field, but it
 * does so inline; a schema for a single radio group would be ceremony.
 */

export {};
