package no.nav.k9.sak.ytelse.pleiepengerbarn.medisinsk;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktUtlederHolder;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.behandling.steg.avklarfakta.KontrollerFaktaUtledereTjenesteImpl;

@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
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
