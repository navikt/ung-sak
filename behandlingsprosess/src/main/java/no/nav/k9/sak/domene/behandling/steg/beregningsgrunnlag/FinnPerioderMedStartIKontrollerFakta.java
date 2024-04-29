package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.InntektsmeldingRelevantForVilkårsrevurdering;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.trigger.ProsessTriggereRepository;
import no.nav.k9.sak.trigger.Trigger;
import no.nav.k9.sak.vilkår.PeriodeTilVurdering;
import no.nav.k9.sak.vilkår.VilkårPeriodeFilterProvider;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;

@Dependent
public class FinnPerioderMedStartIKontrollerFakta {


    private final VilkårResultatRepository vilkårResultatRepository;
    private final VilkårPeriodeFilterProvider vilkårPeriodeFilterProvider;
    private final InntektArbeidYtelseTjeneste iayTjeneste;
    private final MottatteDokumentRepository mottatteDokumentRepository;
    private final BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository;
    private final BehandlingRepository behandlingRepository;
    private final ProsessTriggereRepository prosessTriggereRepository;
    private final SkalForlengeAktivitetstatus skalForlengeAktivitetstatus;


    @Inject
    public FinnPerioderMedStartIKontrollerFakta(VilkårResultatRepository vilkårResultatRepository,
                                                VilkårPeriodeFilterProvider vilkårPeriodeFilterProvider,
                                                InntektArbeidYtelseTjeneste iayTjeneste,
                                                MottatteDokumentRepository mottatteDokumentRepository,
                                                BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository,
                                                BehandlingRepository behandlingRepository,
                                                @Any Instance<InntektsmeldingRelevantForVilkårsrevurdering> inntektsmeldingRelevantForBeregningVilkårsvurdering,
                                                ProsessTriggereRepository prosessTriggereRepository) {
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.vilkårPeriodeFilterProvider = vilkårPeriodeFilterProvider;
        this.iayTjeneste = iayTjeneste;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.beregningPerioderGrunnlagRepository = beregningPerioderGrunnlagRepository;
        this.behandlingRepository = behandlingRepository;
        this.prosessTriggereRepository = prosessTriggereRepository;
        this.skalForlengeAktivitetstatus = new SkalForlengeAktivitetstatus(inntektsmeldingRelevantForBeregningVilkårsvurdering);
    }

    /**
     * Finner perioder skal kopiere resultatet fra fastsett skjæringstidspunkt fra forrige behandling/kobling og starte prosessering av nytt beregningsgrunnlag
     * i steget kontroller fakta beregning
     *
     * @param ref          Behandlingreferanse
     * @param allePerioder Alle perioder (vilkårsperioder)
     * @return Perioder med start i kontroller fakta beregning
     */
    public NavigableSet<PeriodeTilVurdering> finnPerioder(BehandlingReferanse ref,
                                                          NavigableSet<PeriodeTilVurdering> allePerioder) {
        return skalForlengeAktivitetstatus.finnPerioderForForlengelseAvStatus(lagInput(ref, allePerioder));
    }

    private SkalForlengeAktivitetstatus.SkalForlengeStatusInput lagInput(BehandlingReferanse ref, NavigableSet<PeriodeTilVurdering> perioderTilVurderingIBeregning) {
        return new SkalForlengeAktivitetstatus.SkalForlengeStatusInput(
            ref,
            BehandlingReferanse.fra(behandlingRepository.hentBehandling(ref.getOriginalBehandlingId().orElseThrow())),
            iayTjeneste.hentUnikeInntektsmeldingerForSak(ref.getSaksnummer()),
            finnMottatteInntektsmeldinger(ref),
            finnPerioderTilVurderingFraProsesstrigger(ref),
            finnPerioderTilVurderingIOpptjening(ref, perioderTilVurderingIBeregning),
            perioderTilVurderingIBeregning,
            finnInnvilgedePerioderForrigeBehandling(ref),
            beregningPerioderGrunnlagRepository.hentGrunnlag(ref.getBehandlingId()).stream()
                .flatMap(gr -> gr.getKompletthetPerioder().stream())
                .collect(Collectors.toSet()),
            beregningPerioderGrunnlagRepository.getInitiellVersjon(ref.getBehandlingId()).stream()
                .flatMap(gr -> gr.getKompletthetPerioder().stream())
                .collect(Collectors.toSet())
        );
    }


    private NavigableSet<PeriodeTilVurdering> finnPerioderTilVurderingIOpptjening(BehandlingReferanse ref, Set<PeriodeTilVurdering> perioderTilVurdering) {
        var intervaller = perioderTilVurdering.stream().map(PeriodeTilVurdering::getPeriode).collect(Collectors.toSet());
        var periodeFilter = vilkårPeriodeFilterProvider.getFilter(ref);
        periodeFilter.ignorerAvslåttePerioder();
        var perioderTilVurderingIOpptjening = periodeFilter.filtrerPerioder(intervaller, VilkårType.OPPTJENINGSVILKÅRET);
        return perioderTilVurderingIOpptjening;
    }


    private NavigableSet<DatoIntervallEntitet> finnPerioderTilVurderingFraProsesstrigger(BehandlingReferanse ref) {
        return prosessTriggereRepository.hentGrunnlag(ref.getBehandlingId())
            .stream()
            .flatMap(it -> it.getTriggere().stream())
            .filter(t -> t.getÅrsak().equals(BehandlingÅrsakType.RE_ENDRING_BEREGNINGSGRUNNLAG))
            .map(Trigger::getPeriode)
            .collect(Collectors.toCollection(TreeSet::new));
    }

    private List<MottattDokument> finnMottatteInntektsmeldinger(BehandlingReferanse ref) {
        return mottatteDokumentRepository.hentGyldigeDokumenterMedFagsakId(ref.getFagsakId())
            .stream()
            .filter(it -> Objects.equals(Brevkode.INNTEKTSMELDING, it.getType()))
            .toList();
    }

    private NavigableSet<DatoIntervallEntitet> finnInnvilgedePerioderForrigeBehandling(BehandlingReferanse ref) {
        return vilkårResultatRepository.hentHvisEksisterer(ref.getOriginalBehandlingId().orElseThrow()).orElseThrow()
            .getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR)
            .stream()
            .flatMap(v -> v.getPerioder().stream())
            .filter(p -> p.getGjeldendeUtfall().equals(Utfall.OPPFYLT))
            .map(VilkårPeriode::getPeriode)
            .collect(Collectors.toCollection(TreeSet::new));
    }


}
