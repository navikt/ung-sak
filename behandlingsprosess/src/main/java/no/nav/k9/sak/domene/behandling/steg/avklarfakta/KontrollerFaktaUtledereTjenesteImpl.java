package no.nav.k9.sak.domene.behandling.steg.avklarfakta;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtleder;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederHolder;
import no.nav.k9.sak.domene.medlem.kontrollerfakta.AksjonspunktutlederForMedisinskvilkår;
import no.nav.k9.sak.domene.medlem.kontrollerfakta.AksjonspunktutlederForMedlemskap;

@ApplicationScoped
class KontrollerFaktaUtledereTjenesteImpl implements KontrollerFaktaUtledere {

    @Inject
    KontrollerFaktaUtledereTjenesteImpl() {
    }

    // Legg til aksjonspunktutledere som er felles for Førstegangsbehandling og Revurdering
    protected List<AksjonspunktUtleder> leggTilFellesutledere(@SuppressWarnings("unused") BehandlingReferanse ref) {
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
        utlederHolder.leggTil(AksjonspunktutlederForMedlemskap.class)
            .leggTil(AksjonspunktUtlederForTilleggsopplysninger.class)
            .leggTil(AksjonspunktutlederForMedisinskvilkår.class);
    }
}
