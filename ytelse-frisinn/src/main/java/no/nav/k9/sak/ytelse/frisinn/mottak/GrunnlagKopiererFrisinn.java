package no.nav.k9.sak.ytelse.frisinn.mottak;

import java.util.EnumSet;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.abakus.iaygrunnlag.request.Dataset;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandling.revurdering.GrunnlagKopierer;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
@FagsakYtelseTypeRef("FRISINN")
public class GrunnlagKopiererFrisinn implements GrunnlagKopierer {

    private PersonopplysningRepository personopplysningRepository;
    private MedlemskapRepository medlemskapRepository;
    private UttakRepository uttakRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository;
    private Boolean toggletVilkårsperioder;

    public GrunnlagKopiererFrisinn() {
        // for CDI proxy
    }

    @Inject
    public GrunnlagKopiererFrisinn(BehandlingRepositoryProvider repositoryProvider,
                                   UttakRepository uttakRepository,
                                   InntektArbeidYtelseTjeneste iayTjeneste,
                                   BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository,
                                   @KonfigVerdi(value = "FRISINN_VILKARSPERIODER", defaultVerdi = "true") Boolean toggletVilkårsperioder) {
        this.uttakRepository = uttakRepository;
        this.iayTjeneste = iayTjeneste;
        this.personopplysningRepository = repositoryProvider.getPersonopplysningRepository();
        this.medlemskapRepository = repositoryProvider.getMedlemskapRepository();
        this.beregningPerioderGrunnlagRepository = beregningPerioderGrunnlagRepository;
        this.toggletVilkårsperioder = toggletVilkårsperioder;
    }


    @Override
    public void kopierGrunnlagVedManuellOpprettelse(Behandling original, Behandling ny) {
        var originalBehandlingId = original.getId();
        var nyBehandlingId = ny.getId();
        personopplysningRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
        medlemskapRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
        uttakRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);

        // gjør til slutt, innebærer kall til abakus
        iayTjeneste.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
    }


    @Override
    public void kopierGrunnlagVedAutomatiskOpprettelse(Behandling original, Behandling ny) {
        var originalBehandlingId = original.getId();
        var nyBehandlingId = ny.getId();
        personopplysningRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
        medlemskapRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
        uttakRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
        if (toggletVilkårsperioder) {
            beregningPerioderGrunnlagRepository.kopier(originalBehandlingId, nyBehandlingId);
        }

        // gjør til slutt, innebærer kall til abakus
        // Delvis kopiering hvor alt unntatt oppgitte opptjening fra søknad kopieres over
        // Den sistnevnte delen av grunnlaget må legges til i mottak av selve søknaden
        var datasetMinusOppgittOpptjening = EnumSet.allOf(Dataset.class);
        datasetMinusOppgittOpptjening.remove(Dataset.OPPGITT_OPPTJENING);
        iayTjeneste.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId, datasetMinusOppgittOpptjening);
    }


    @Override
    public List<AksjonspunktDefinisjon> getApForManuellRevurdering() {
        return List.of(AksjonspunktDefinisjon.OVERSTYRING_FRISINN_OPPGITT_OPPTJENING);
    }
}
