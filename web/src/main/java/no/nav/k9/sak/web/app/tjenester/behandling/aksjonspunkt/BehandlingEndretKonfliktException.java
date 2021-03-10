package no.nav.k9.sak.web.app.tjenester.behandling.aksjonspunkt;

import no.nav.k9.felles.exception.FunksjonellException;
import no.nav.k9.felles.feil.FunksjonellFeil;

public class BehandlingEndretKonfliktException extends FunksjonellException {

    public BehandlingEndretKonfliktException(FunksjonellFeil feil) {
        super(feil);
    }

}
