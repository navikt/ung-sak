package no.nav.k9.sak.mottak.dokumentmottak;

import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.feil.Feil;

public class DokumentValideringException extends TekniskException {
    public DokumentValideringException(Feil feil) {
        super(feil);
    }
}
