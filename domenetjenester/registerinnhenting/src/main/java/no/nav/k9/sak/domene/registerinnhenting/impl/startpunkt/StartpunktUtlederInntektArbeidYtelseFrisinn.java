package no.nav.k9.sak.domene.registerinnhenting.impl.startpunkt;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.hendelser.StartpunktType;
import no.nav.k9.sak.domene.registerinnhenting.GrunnlagRef;
import no.nav.k9.sak.domene.registerinnhenting.StartpunktUtleder;

@ApplicationScoped
@FagsakYtelseTypeRef("FRISINN")
@GrunnlagRef("InntektArbeidYtelseGrunnlag")
class StartpunktUtlederInntektArbeidYtelseFrisinn implements StartpunktUtleder {

    private String klassenavn = this.getClass().getSimpleName();
    private BehandlingRepository behandlingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

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
