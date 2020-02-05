package no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt;

import no.nav.vedtak.exception.FunksjonellException;
import no.nav.vedtak.feil.FunksjonellFeil;

public class BehandlingEndretKonfliktException extends FunksjonellException {

    public BehandlingEndretKonfliktException(FunksjonellFeil feil) {
        super(feil);
    }

}
