package no.nav.k9.sak.mottak.dokumentmottak;

import no.nav.vedtak.log.util.LoggerUtils;

public class DokumentValideringException extends RuntimeException {
    public DokumentValideringException(String feilmelding) {
        super(feilmelding);
    }

    public DokumentValideringException(String feilmelding, Exception cause) {
        super(feilmelding, cause);
    }

    public String getMessageWithoutLinebreaks() {
        return LoggerUtils.toStringWithoutLineBreaks(getMessage());
    }
}
