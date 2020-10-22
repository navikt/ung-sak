package no.nav.k9.sak.ytelse.unntaksbehandling.registerinnhenting;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.hendelser.StartpunktType;
import no.nav.k9.sak.domene.registerinnhenting.StartpunktUtleder;
import no.nav.k9.sak.domene.registerinnhenting.impl.startpunkt.FellesStartpunktUtlederLogger;

@ApplicationScoped
@FagsakYtelseTypeRef
@BehandlingTypeRef("BT-010")
//@GrunnlagRef("InntektArbeidYtelseGrunnlag")
class StartpunktUtlederInntektArbeidUnntaksbehandling implements StartpunktUtleder {


    @Inject
    public StartpunktUtlederInntektArbeidUnntaksbehandling() {
        // For CDI
    }

    @Override
    public StartpunktType utledStartpunkt(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2) {
        FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(this.getClass().getSimpleName(), StartpunktType.KONTROLLER_FAKTA, "iay", grunnlagId1, grunnlagId2);
        return StartpunktType.KONTROLLER_FAKTA;
    }

}
