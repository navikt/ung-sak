package no.nav.k9.sak.ytelse.pleiepengerbarn.medisinsk;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktUtlederHolder;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.behandling.steg.avklarfakta.KontrollerFaktaUtledereTjenesteImpl;

@FagsakYtelseTypeRef("PSB")
@BehandlingTypeRef
@ApplicationScoped
class PleiepengerBarnKontrollerFaktaUtledereTjeneste extends KontrollerFaktaUtledereTjenesteImpl {

    @Inject
    PleiepengerBarnKontrollerFaktaUtledereTjeneste() {
    }

    @Override
    protected AksjonspunktUtlederHolder leggTilFellesutledere(BehandlingReferanse ref) {
        var utlederHolder = super.leggTilFellesutledere(ref);        
        return utlederHolder;
    }


}
