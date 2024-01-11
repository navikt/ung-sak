package no.nav.k9.sak.domene.behandling.steg.avklarfakta;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktUtleder;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktUtlederHolder;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.medlem.kontrollerfakta.AksjonspunktutlederForMedlemskap;

@FagsakYtelseTypeRef
@BehandlingTypeRef
@ApplicationScoped
public class KontrollerFaktaUtledereTjenesteImpl implements KontrollerFaktaUtledere {

    @Override
    public List<AksjonspunktUtleder> utledUtledereFor(BehandlingReferanse ref) {
        var utlederHolder = new AksjonspunktUtlederHolder();
        utlederHolder.leggTil(AksjonspunktutlederForMedlemskap.class);
        utlederHolder.leggTil(AksjonspunktUtlederForTilleggsopplysninger.class);
        return utlederHolder.getUtledere();
    }

}
