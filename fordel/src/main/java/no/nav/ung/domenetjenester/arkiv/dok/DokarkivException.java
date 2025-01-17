package no.nav.ung.domenetjenester.arkiv.dok;

public class DokarkivException extends RuntimeException {
    public DokarkivException(String message) {
        super(message);
    }

    public DokarkivException(String message, Exception cause) {
        super(message, cause);
    }
}
