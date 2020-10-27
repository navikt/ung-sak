package no.nav.k9.sak.ytelse.frisinn.registerinnhenting;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.hendelser.StartpunktType;
import no.nav.k9.sak.domene.registerinnhenting.GrunnlagRef;
import no.nav.k9.sak.domene.registerinnhenting.StartpunktUtleder;

@ApplicationScoped
@FagsakYtelseTypeRef("FRISINN")
@BehandlingTypeRef
@GrunnlagRef("InntektArbeidYtelseGrunnlag")
class StartpunktUtlederInntektArbeidYtelseFrisinn implements StartpunktUtleder {


    @Inject
    public StartpunktUtlederInntektArbeidYtelseFrisinn() {
        // For CDI
    }

    @Override
    public StartpunktType utledStartpunkt(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2) {
        // IAY-endringer påvirker ikke Frisinn
        return StartpunktType.UDEFINERT;
    }

}
