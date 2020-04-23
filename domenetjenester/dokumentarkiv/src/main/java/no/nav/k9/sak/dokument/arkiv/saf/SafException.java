package no.nav.k9.sak.dokument.arkiv.saf;

public class SafException extends RuntimeException {
    public SafException(String message) {
        super(message);
    }

    public SafException(String message, Exception cause) {
        super(message, cause);
    }
}
