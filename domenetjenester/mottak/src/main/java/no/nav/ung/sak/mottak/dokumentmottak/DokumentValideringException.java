package no.nav.ung.sak.mottak.dokumentmottak;

import no.nav.k9.felles.log.util.LoggerUtils;

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
