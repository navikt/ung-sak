package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.endring;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.hendelser.StartpunktType;
import no.nav.k9.sak.domene.registerinnhenting.EndringStartpunktUtleder;
import no.nav.k9.sak.domene.registerinnhenting.GrunnlagRef;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagRepository;

@ApplicationScoped
@GrunnlagRef("SykdomGrunnlag")
@FagsakYtelseTypeRef("PSB")
class StartpunktUtlederSykdom implements EndringStartpunktUtleder {

    private SykdomGrunnlagRepository sykdomGrunnlagRepository;

    StartpunktUtlederSykdom() {
        // For CDI
    }

    @Inject
    StartpunktUtlederSykdom(SykdomGrunnlagRepository sykdomGrunnlagRepository) {
        this.sykdomGrunnlagRepository = sykdomGrunnlagRepository;
    }

    @Override
    public StartpunktType utledStartpunkt(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2) {
        // TODO: to be implemented
        return StartpunktType.UDEFINERT;
    }

}
