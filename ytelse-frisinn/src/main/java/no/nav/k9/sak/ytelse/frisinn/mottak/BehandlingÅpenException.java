package no.nav.k9.sak.ytelse.frisinn.mottak;

import no.nav.vedtak.exception.IntegrasjonException;
import no.nav.vedtak.feil.Feil;

public class BehandlingÅpenException extends IntegrasjonException {
    public BehandlingÅpenException(Feil feil) {
        super(feil);
    }
}
