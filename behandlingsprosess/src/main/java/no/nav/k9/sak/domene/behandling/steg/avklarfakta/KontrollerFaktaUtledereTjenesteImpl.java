package no.nav.k9.sak.domene.behandling.steg.avklarfakta;

import java.util.List; 

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktUtleder;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktUtlederHolder;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.medlem.kontrollerfakta.AksjonspunktutlederForMedlemskap;

@FagsakYtelseTypeRef
@ApplicationScoped
public class KontrollerFaktaUtledereTjenesteImpl implements KontrollerFaktaUtledere {

    @Inject
    protected KontrollerFaktaUtledereTjenesteImpl() {
    }

    // Legg til aksjonspunktutledere som er felles for Førstegangsbehandling og Revurdering
    protected AksjonspunktUtlederHolder leggTilFellesutledere(BehandlingReferanse ref) {
        var utlederHolder = new AksjonspunktUtlederHolder();

        // Legger til utledere som alltid skal kjøres
        leggTilStandardUtledere(utlederHolder);

        if (FagsakYtelseType.OMP.equals(ref.getFagsakYtelseType())) {
            utlederHolder.leggTil(AksjonspunktutlederForAlder.class);
        }

        return utlederHolder;
    }

    @Override
    public final List<AksjonspunktUtleder> utledUtledereFor(BehandlingReferanse ref) {
        return leggTilFellesutledere(ref).getUtledere();
    }

    private void leggTilStandardUtledere(AksjonspunktUtlederHolder utlederHolder) {
        utlederHolder.leggTil(AksjonspunktutlederForMedlemskap.class)
            .leggTil(AksjonspunktUtlederForTilleggsopplysninger.class);
    }
}
