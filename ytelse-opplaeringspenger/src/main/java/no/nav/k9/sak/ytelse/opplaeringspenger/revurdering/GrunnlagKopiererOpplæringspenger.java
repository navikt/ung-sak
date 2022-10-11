package no.nav.k9.sak.ytelse.opplaeringspenger.revurdering;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.søknadsfrist.AvklartSøknadsfristRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.omsorg.OmsorgenForGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov.PleiebehovResultatRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleietrengende.død.RettPleiepengerVedDødRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.revurdering.GrunnlagKopiererPleiepenger;

@ApplicationScoped
@FagsakYtelseTypeRef(OPPLÆRINGSPENGER)
public class GrunnlagKopiererOpplæringspenger extends GrunnlagKopiererPleiepenger {

    private VurdertOpplæringRepository vurdertOpplæringRepository;

    public GrunnlagKopiererOpplæringspenger() {
    }

    @Inject
    public GrunnlagKopiererOpplæringspenger(BehandlingRepositoryProvider repositoryProvider,
                                            BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository,
                                            AvklartSøknadsfristRepository avklartSøknadsfristRepository,
                                            PleiebehovResultatRepository pleiebehovResultatRepository,
                                            SøknadsperiodeRepository søknadsperiodeRepository,
                                            UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository,
                                            OmsorgenForGrunnlagRepository omsorgenForGrunnlagRepository,
                                            RettPleiepengerVedDødRepository rettPleiepengerVedDødRepository,
                                            InntektArbeidYtelseTjeneste iayTjeneste,
                                            VurdertOpplæringRepository vurdertOpplæringRepository) {
        super(repositoryProvider,
            beregningPerioderGrunnlagRepository,
            avklartSøknadsfristRepository,
            pleiebehovResultatRepository,
            søknadsperiodeRepository,
            uttakPerioderGrunnlagRepository,
            omsorgenForGrunnlagRepository,
            rettPleiepengerVedDødRepository,
            iayTjeneste);
        this.vurdertOpplæringRepository = vurdertOpplæringRepository;
    }

    @Override
    public void kopierGrunnlagVedManuellOpprettelse(Behandling original, Behandling ny) {
        Long originalBehandlingId = original.getId();
        Long nyBehandlingId = ny.getId();
        vurdertOpplæringRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
        super.kopierGrunnlagVedManuellOpprettelse(original, ny);
    }

    @Override
    public void kopierGrunnlagVedAutomatiskOpprettelse(Behandling original, Behandling ny) {
        kopierGrunnlagVedManuellOpprettelse(original, ny);
    }
}
