package no.nav.foreldrepenger.behandling.steg.avklarfakta.fp;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtleder;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederHolder;
import no.nav.foreldrepenger.behandling.steg.avklarfakta.AksjonspunktUtlederForTilleggsopplysninger;
import no.nav.foreldrepenger.behandling.steg.avklarfakta.KontrollerFaktaUtledere;
import no.nav.foreldrepenger.domene.medlem.kontrollerfakta.AksjonspunktutlederForMedlemskapSkjæringstidspunkt;

@ApplicationScoped
class KontrollerFaktaUtledereTjenesteImpl implements KontrollerFaktaUtledere {

    @Inject
    KontrollerFaktaUtledereTjenesteImpl() {
    }

    // Legg til aksjonspunktutledere som er felles for Førstegangsbehandling og Revurdering
    protected List<AksjonspunktUtleder> leggTilFellesutledere(BehandlingReferanse ref) {
        var utlederHolder = new AksjonspunktUtlederHolder();

        // Legger til utledere som alltid skal kjøres
        leggTilStandardUtledere(utlederHolder);

        return utlederHolder.getUtledere();
    }

    @Override
    public List<AksjonspunktUtleder> utledUtledereFor(BehandlingReferanse ref) {
        return leggTilFellesutledere(ref);
    }

    private void leggTilStandardUtledere(AksjonspunktUtlederHolder utlederHolder) {
        utlederHolder.leggTil(AksjonspunktutlederForMedlemskapSkjæringstidspunkt.class)
            .leggTil(AksjonspunktUtlederForTilleggsopplysninger.class)
            ;
    }
}
