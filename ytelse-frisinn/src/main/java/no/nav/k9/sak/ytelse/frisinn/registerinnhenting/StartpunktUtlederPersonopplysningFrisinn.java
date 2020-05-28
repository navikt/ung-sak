package no.nav.k9.sak.ytelse.frisinn.registerinnhenting;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.hendelser.StartpunktType;
import no.nav.k9.sak.domene.registerinnhenting.GrunnlagRef;
import no.nav.k9.sak.domene.registerinnhenting.StartpunktUtleder;

@ApplicationScoped
@FagsakYtelseTypeRef("FRISINN")
@GrunnlagRef("PersonInformasjon")
class StartpunktUtlederPersonopplysningFrisinn implements StartpunktUtleder {


    @Inject
    StartpunktUtlederPersonopplysningFrisinn() {
        // For CDI
    }

    @Override
    public StartpunktType utledStartpunkt(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2) {
        // Personinfo-endringer påvirker ikke Frisinn
        return StartpunktType.UDEFINERT;
    }
}
