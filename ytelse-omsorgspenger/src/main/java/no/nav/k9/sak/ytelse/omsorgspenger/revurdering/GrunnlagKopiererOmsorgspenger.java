package no.nav.k9.sak.ytelse.omsorgspenger.revurdering;

import java.util.EnumSet;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.abakus.iaygrunnlag.request.Dataset;
import no.nav.k9.sak.behandling.revurdering.GrunnlagKopierer;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OmsorgspengerGrunnlagRepository;

@ApplicationScoped
@FagsakYtelseTypeRef("OMP")
public class GrunnlagKopiererOmsorgspenger implements GrunnlagKopierer {

    private PersonopplysningRepository personopplysningRepository;
    private MedlemskapRepository medlemskapRepository;
    private OmsorgspengerGrunnlagRepository omsorgspengerGrunnlagRepository;
    private BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste;

    public GrunnlagKopiererOmsorgspenger() {
        // for CDI proxy
    }

    @Inject
    public GrunnlagKopiererOmsorgspenger(BehandlingRepositoryProvider repositoryProvider,
                                         OmsorgspengerGrunnlagRepository omsorgspengerGrunnlagRepository,
                                         BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository,
                                         InntektArbeidYtelseTjeneste iayTjeneste) {
        this.omsorgspengerGrunnlagRepository = omsorgspengerGrunnlagRepository;
        this.beregningPerioderGrunnlagRepository = beregningPerioderGrunnlagRepository;

        this.iayTjeneste = iayTjeneste;
        this.personopplysningRepository = repositoryProvider.getPersonopplysningRepository();
        this.medlemskapRepository = repositoryProvider.getMedlemskapRepository();
    }


    @Override
    public void kopierGrunnlagVedManuellOpprettelse(Behandling original, Behandling ny) {
        Long originalBehandlingId = original.getId();
        Long nyBehandlingId = ny.getId();
        personopplysningRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
        medlemskapRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);

        omsorgspengerGrunnlagRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
        beregningPerioderGrunnlagRepository.kopier(originalBehandlingId, nyBehandlingId, true);

        // gjør til slutt, innebærer kall til abakus
        // TODO: Siste søknad er ikke nødvendigvis på forrige behandling. Derfor kan den bli mistet her
        iayTjeneste.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
    }

    @Override
    public void kopierGrunnlagVedAutomatiskOpprettelse(Behandling original, Behandling ny) {
        Long originalBehandlingId = original.getId();
        Long nyBehandlingId = ny.getId();
        personopplysningRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
        medlemskapRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);

        omsorgspengerGrunnlagRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
        beregningPerioderGrunnlagRepository.kopier(originalBehandlingId, nyBehandlingId, true);

        // gjør til slutt, innebærer kall til abakus
        // Oppgitt opptjening må oppgis i søknad, kopieres derfor ikke fra forrige
        var datasetMinusOppgittOpptjening = EnumSet.allOf(Dataset.class);
        datasetMinusOppgittOpptjening.remove(Dataset.OPPGITT_OPPTJENING);
        iayTjeneste.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId, datasetMinusOppgittOpptjening);
    }

}
