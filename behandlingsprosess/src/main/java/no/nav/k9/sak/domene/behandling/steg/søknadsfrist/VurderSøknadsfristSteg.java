package no.nav.k9.sak.domene.behandling.steg.søknadsfrist;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;

@BehandlingStegRef(kode = "VURDER_SØKNADSFRIST")
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class VurderSøknadsfristSteg implements BehandlingSteg {

    @Inject
    public VurderSøknadsfristSteg() {

    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {

        // TODO: Hent ut perioder til vurdering og map til standar modell

        // TODO: Vurder perioder
        //

        // TODO: Lagre vurdering
        // Detaljer hvertfall
        // TODO: Lagre vilkår?

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }
}
