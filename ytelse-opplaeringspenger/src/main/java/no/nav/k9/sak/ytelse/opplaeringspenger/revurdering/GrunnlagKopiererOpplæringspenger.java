package no.nav.k9.sak.ytelse.opplaeringspenger.revurdering;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.sak.behandling.revurdering.GrunnlagKopierer;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.revurdering.GrunnlagKopiererPleiepenger;

@ApplicationScoped
@FagsakYtelseTypeRef(OPPLÆRINGSPENGER)
public class GrunnlagKopiererOpplæringspenger implements GrunnlagKopierer {

    private VurdertOpplæringRepository vurdertOpplæringRepository;
    private GrunnlagKopiererPleiepenger grunnlagKopiererPleiepenger;

    public GrunnlagKopiererOpplæringspenger() {
    }

    @Inject
    public GrunnlagKopiererOpplæringspenger(@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN) GrunnlagKopiererPleiepenger grunnlagKopiererPleiepenger,
                                            VurdertOpplæringRepository vurdertOpplæringRepository) {
        this.vurdertOpplæringRepository = vurdertOpplæringRepository;
        this.grunnlagKopiererPleiepenger = grunnlagKopiererPleiepenger;
    }

    @Override
    public void kopierGrunnlagVedManuellOpprettelse(Behandling original, Behandling ny) {
        Long originalBehandlingId = original.getId();
        Long nyBehandlingId = ny.getId();
        vurdertOpplæringRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);

        // Denne må ligge til slutt pga IAY kopien som ligger her
        grunnlagKopiererPleiepenger.kopierGrunnlagVedManuellOpprettelse(original, ny);
    }

    @Override
    public void kopierGrunnlagVedAutomatiskOpprettelse(Behandling original, Behandling ny) {
        kopierGrunnlagVedManuellOpprettelse(original, ny);
    }
}
