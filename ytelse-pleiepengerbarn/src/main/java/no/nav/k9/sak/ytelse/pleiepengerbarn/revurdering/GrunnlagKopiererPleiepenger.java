package no.nav.k9.sak.ytelse.pleiepengerbarn.revurdering;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandling.revurdering.GrunnlagKopierer;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.omsorg.OmsorgenForGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov.PleiebehovResultatRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleietrengende.død.RettPleiepengerVedDødRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsynGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderGrunnlagRepository;

@ApplicationScoped
@FagsakYtelseTypeRef("PSB")
public class GrunnlagKopiererPleiepenger implements GrunnlagKopierer {

    private PersonopplysningRepository personopplysningRepository;
    private BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository;
    private MedlemskapRepository medlemskapRepository;
    private PleiebehovResultatRepository pleiebehovResultatRepository;
    private SøknadsperiodeRepository søknadsperiodeRepository;
    private UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository;
    private OmsorgenForGrunnlagRepository omsorgenForGrunnlagRepository;
    private UnntakEtablertTilsynGrunnlagRepository unntakEtablertTilsynGrunnlagRepository;
    private RettPleiepengerVedDødRepository rettPleiepengerVedDødRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste;

    public GrunnlagKopiererPleiepenger() {
        // for CDI proxy
    }

    @Inject
    public GrunnlagKopiererPleiepenger(BehandlingRepositoryProvider repositoryProvider,
                                       BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository, UttakRepository uttakRepository,
                                       PleiebehovResultatRepository pleiebehovResultatRepository,
                                       SøknadsperiodeRepository søknadsperiodeRepository,
                                       UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository,
                                       OmsorgenForGrunnlagRepository omsorgenForGrunnlagRepository,
                                       UnntakEtablertTilsynGrunnlagRepository unntakEtablertTilsynGrunnlagRepository,
                                       RettPleiepengerVedDødRepository rettPleiepengerVedDødRepository,
                                       InntektArbeidYtelseTjeneste iayTjeneste) {
        this.beregningPerioderGrunnlagRepository = beregningPerioderGrunnlagRepository;
        this.iayTjeneste = iayTjeneste;
        this.pleiebehovResultatRepository = pleiebehovResultatRepository;
        this.søknadsperiodeRepository = søknadsperiodeRepository;
        this.uttakPerioderGrunnlagRepository = uttakPerioderGrunnlagRepository;
        this.omsorgenForGrunnlagRepository = omsorgenForGrunnlagRepository;
        this.unntakEtablertTilsynGrunnlagRepository = unntakEtablertTilsynGrunnlagRepository;
        this.personopplysningRepository = repositoryProvider.getPersonopplysningRepository();
        this.medlemskapRepository = repositoryProvider.getMedlemskapRepository();
    }


    @Override
    public void kopierGrunnlagVedManuellOpprettelse(Behandling original, Behandling ny) {
        Long originalBehandlingId = original.getId();
        Long nyBehandlingId = ny.getId();
        personopplysningRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
        medlemskapRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);

        søknadsperiodeRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
        uttakPerioderGrunnlagRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
        pleiebehovResultatRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
        beregningPerioderGrunnlagRepository.kopier(originalBehandlingId, nyBehandlingId, true);
        omsorgenForGrunnlagRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
        unntakEtablertTilsynGrunnlagRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
        rettPleiepengerVedDødRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);

        // gjør til slutt, innebærer kall til abakus
        iayTjeneste.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
    }

    @Override
    public void kopierGrunnlagVedAutomatiskOpprettelse(Behandling original, Behandling ny) {
        kopierGrunnlagVedManuellOpprettelse(original, ny);
    }

}
