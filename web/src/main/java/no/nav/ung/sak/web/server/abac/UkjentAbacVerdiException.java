package no.nav.ung.sak.web.server.abac;

/**
 * Lager egen exception for når ikke alt er som forventet med abac-verdier, for å kunne håndtere i egen ExceptionMapper
 */
public abstract class UkjentAbacVerdiException extends RuntimeException {

    protected UkjentAbacVerdiException(String message) {
        super(message);
    }

    protected UkjentAbacVerdiException(String message, Throwable cause) {
        super(message, cause);
    }
}
