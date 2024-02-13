package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.InntektsmeldingRelevantForVilkårsrevurdering;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
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
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.trigger.ProsessTriggereRepository;
import no.nav.k9.sak.trigger.Trigger;
import no.nav.k9.sak.vilkår.PeriodeTilVurdering;
import no.nav.k9.sak.vilkår.VilkårPeriodeFilterProvider;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.beregning.grunnlag.KompletthetPeriode;

@Dependent
public class FinnPerioderMedStartIKontrollerFakta {


    private final VilkårResultatRepository vilkårResultatRepository;
    private final VilkårPeriodeFilterProvider vilkårPeriodeFilterProvider;
    private final HarEndretInntektsmeldingVurderer harEndretInntektsmeldingVurderer;
    private final InntektArbeidYtelseTjeneste iayTjeneste;
    private final MottatteDokumentRepository mottatteDokumentRepository;
    private final BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository;
    private final ProsessTriggereRepository prosessTriggereRepository;


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
        this.prosessTriggereRepository = prosessTriggereRepository;
        this.harEndretInntektsmeldingVurderer = new HarEndretInntektsmeldingVurderer(
            behandlingRepository,
            getInntektsmeldingFilter(inntektsmeldingRelevantForBeregningVilkårsvurdering),
            FinnPerioderMedStartIKontrollerFakta::erEndret);
    }

    private static HarEndretInntektsmeldingVurderer.InntektsmeldingFilter getInntektsmeldingFilter(Instance<InntektsmeldingRelevantForVilkårsrevurdering> inntektsmeldingRelevantForBeregningVilkårsvurdering) {
        return (BehandlingReferanse referanse, Collection<Inntektsmelding> sakInntektsmeldinger, DatoIntervallEntitet vilkårsPeriode) ->
            InntektsmeldingRelevantForVilkårsrevurdering.finnTjeneste(inntektsmeldingRelevantForBeregningVilkårsvurdering, VilkårType.BEREGNINGSGRUNNLAGVILKÅR, referanse.getFagsakYtelseType()).begrensInntektsmeldinger(referanse, sakInntektsmeldinger, vilkårsPeriode);
    }

    /**
     * Finner perioder skal kopiere resultatet fra fastsett skjæringstidspunkt fra forrige behandling/kobling og starte prosessering av nytt beregningsgrunnlag
     * i steget kontroller fakta beregning
     *
     * @param ref                          Behandlingreferanse
     * @param allePerioder                 Alle perioder (vilkårsperioder)
     * @param forlengelseperioderBeregning Perioder med forlengelse i beregning
     * @return Perioder med start i kontroller fakta beregning
     */
    public NavigableSet<PeriodeTilVurdering> finnPerioder(BehandlingReferanse ref,
                                                          NavigableSet<PeriodeTilVurdering> allePerioder,
                                                          Set<PeriodeTilVurdering> forlengelseperioderBeregning) {

        return finnPerioderForForlengelseAvStatus(ref, forlengelseperioderBeregning, allePerioder);
    }

    private NavigableSet<PeriodeTilVurdering> finnPerioderForForlengelseAvStatus(BehandlingReferanse ref,
                                                                                 Set<PeriodeTilVurdering> forlengelseperioderBeregning,
                                                                                 Set<PeriodeTilVurdering> perioderTilVurdering) {

        var intervaller = perioderTilVurdering.stream().map(PeriodeTilVurdering::getPeriode).collect(Collectors.toSet());
        var oppfyltePerioderForrigeBehandlingTidslinje = finnOppfylteVilkårsperioderForrigeBehandlingTidslinje(ref, perioderTilVurdering);
        var forlengelserIOpptjeningTidslinje = finnForlengelserIOpptjeningTidslinje(ref, intervaller);
        var prosesstriggerTidslinje = finnProsesstriggerTidslinje(ref);
        var forlengelserIBeregningTidslinje = finnForlengelserIBeregningTidslinje(forlengelseperioderBeregning);
        var utenEndringIInntektsmeldingTidslinje = finnTidslinjeForInntektsmeldingUtenEndring(ref, intervaller);
        var ingenEndringIKompletthetTidslinje = finnTidslinjeForKompletthetUtenEndring(ref, intervaller);

        var forlengetStatusTidslinje = TidslinjeUtil.tilTidslinjeKomprimert(intervaller)
            .intersection(forlengelserIOpptjeningTidslinje)
            .intersection(oppfyltePerioderForrigeBehandlingTidslinje)
            .intersection(utenEndringIInntektsmeldingTidslinje)
            .intersection(ingenEndringIKompletthetTidslinje)
            .disjoint(forlengelserIBeregningTidslinje)
            .disjoint(prosesstriggerTidslinje);

        var forlengeletStatusPerioder = TidslinjeUtil.tilDatoIntervallEntiteter(forlengetStatusTidslinje);
        return perioderTilVurdering.stream()
            .filter(p -> forlengeletStatusPerioder.stream().anyMatch(it -> it.overlapper(p.getPeriode())))
            .collect(Collectors.toCollection(TreeSet::new));
    }

    private LocalDateTimeline<Boolean> finnTidslinjeForKompletthetUtenEndring(BehandlingReferanse ref, Set<DatoIntervallEntitet> perioder) {
        var initiellKompletthetPerioder = beregningPerioderGrunnlagRepository.getInitiellVersjon(ref.getBehandlingId()).stream()
            .flatMap(gr -> gr.getKompletthetPerioder().stream())
            .collect(Collectors.toSet());
        var aktiveKompletthetPerioder = beregningPerioderGrunnlagRepository.hentGrunnlag(ref.getBehandlingId()).stream()
            .flatMap(gr -> gr.getKompletthetPerioder().stream())
            .collect(Collectors.toSet());
        return perioder.stream()
            .filter(p -> erKompletthetsvurderingLikForrigeVedtak(aktiveKompletthetPerioder, initiellKompletthetPerioder, p.getFomDato()))
            .map(it -> new LocalDateTimeline<>(it.getFomDato(), it.getTomDato(), true))
            .reduce(LocalDateTimeline.empty(), (t1, t2) -> t1.crossJoin(t2, StandardCombinators::alwaysTrueForMatch));
    }

    private LocalDateTimeline<Boolean> finnTidslinjeForInntektsmeldingUtenEndring(BehandlingReferanse ref, Set<DatoIntervallEntitet> perioder) {
        var inntektsmeldinger = iayTjeneste.hentUnikeInntektsmeldingerForSak(ref.getSaksnummer());
        var mottatteInntektsmeldinger = finnMottatteInntektsmeldinger(ref);
        return perioder.stream()
            .filter((p) -> erInntektsmeldingerLikForrigeVedtak(ref, inntektsmeldinger, mottatteInntektsmeldinger, p))
            .map(it -> new LocalDateTimeline<>(it.getFomDato(), it.getTomDato(), true))
            .reduce(LocalDateTimeline.empty(), (t1, t2) -> t1.crossJoin(t2, StandardCombinators::alwaysTrueForMatch));
    }

    private static LocalDateTimeline<Boolean> finnForlengelserIBeregningTidslinje(Set<PeriodeTilVurdering> forlengelseperioderBeregning) {
        return forlengelseperioderBeregning.stream()
            .map(PeriodeTilVurdering::getPeriode)
            .map(it -> new LocalDateTimeline<>(it.getFomDato(), it.getTomDato(), true))
            .reduce(LocalDateTimeline.empty(), (t1, t2) -> t1.crossJoin(t2, StandardCombinators::alwaysTrueForMatch));
    }

    private LocalDateTimeline<Boolean> finnForlengelserIOpptjeningTidslinje(BehandlingReferanse ref, Set<DatoIntervallEntitet> perioder) {
        var periodeFilter = vilkårPeriodeFilterProvider.getFilter(ref);
        periodeFilter.ignorerAvslåttePerioder();
        return periodeFilter.filtrerPerioder(perioder, VilkårType.OPPTJENINGSVILKÅRET).stream()
            .filter(PeriodeTilVurdering::erForlengelse)
            .map(PeriodeTilVurdering::getPeriode)
            .map(it -> new LocalDateTimeline<>(it.getFomDato(), it.getTomDato(), true))
            .reduce(LocalDateTimeline.empty(), (t1, t2) -> t1.crossJoin(t2, StandardCombinators::alwaysTrueForMatch));
    }

    private LocalDateTimeline<Boolean> finnProsesstriggerTidslinje(BehandlingReferanse ref) {
        return prosessTriggereRepository.hentGrunnlag(ref.getBehandlingId())
            .stream()
            .flatMap(it -> it.getTriggere().stream())
            .filter(t -> t.getÅrsak().equals(BehandlingÅrsakType.RE_ENDRING_BEREGNINGSGRUNNLAG))
            .map(Trigger::getPeriode)
            .map(it -> new LocalDateTimeline<>(it.getFomDato(), it.getTomDato(), true))
            .reduce(LocalDateTimeline.empty(), (t1, t2) -> t1.crossJoin(t2, StandardCombinators::alwaysTrueForMatch));
    }

    private List<MottattDokument> finnMottatteInntektsmeldinger(BehandlingReferanse ref) {
        return mottatteDokumentRepository.hentGyldigeDokumenterMedFagsakId(ref.getFagsakId())
            .stream()
            .filter(it -> Objects.equals(Brevkode.INNTEKTSMELDING, it.getType()))
            .toList();
    }

    private boolean erKompletthetsvurderingLikForrigeVedtak(Set<KompletthetPeriode> aktiveKompletthetPerioder, Set<KompletthetPeriode> initiellKompletthetPerioder, LocalDate skjæringstidspunkt) {
        var initiellPeriode = initiellKompletthetPerioder.stream().filter(it -> it.getSkjæringstidspunkt().equals(skjæringstidspunkt)).findFirst();
        var aktivPeriode = aktiveKompletthetPerioder.stream().filter(it -> it.getSkjæringstidspunkt().equals(skjæringstidspunkt)).findFirst();

        if (aktivPeriode.isPresent()) {
            return initiellPeriode.isPresent() && aktivPeriode.get().getVurdering().equals(initiellPeriode.get().getVurdering());
        } else {
            return initiellPeriode.isEmpty();
        }

    }

    private boolean erInntektsmeldingerLikForrigeVedtak(BehandlingReferanse ref,
                                                        Set<Inntektsmelding> inntektsmeldings,
                                                        List<MottattDokument> mottatteInntektsmeldinger,
                                                        DatoIntervallEntitet periode) {
        return !harEndretInntektsmeldingVurderer.harEndringPåInntektsmeldingerTilBrukForPerioden(ref,
            periode, inntektsmeldings,
            mottatteInntektsmeldinger
        );
    }


    static boolean erEndret(Collection<Inntektsmelding> relevanteInntektsmeldinger, Collection<Inntektsmelding> relevanteInntektsmeldingerForrigeVedtak) {
        var unikeArbeidsforhold = finnUnikeArbeidsforholdIdentifikatorer(relevanteInntektsmeldinger);
        var unikeArbeidsforholdForrigeVedtak = finnUnikeArbeidsforholdIdentifikatorer(relevanteInntektsmeldingerForrigeVedtak);
        var erLikeStore = unikeArbeidsforhold.size() == unikeArbeidsforholdForrigeVedtak.size();
        var inneholderDeSamme = unikeArbeidsforhold.containsAll(unikeArbeidsforholdForrigeVedtak);
        return !(erLikeStore && inneholderDeSamme);
    }

    private static Set<String> finnUnikeArbeidsforholdIdentifikatorer(Collection<Inntektsmelding> relevanteInntektsmeldinger) {
        return relevanteInntektsmeldinger.stream().map(
            im -> im.getArbeidsgiver().getIdentifikator() + im.getArbeidsforholdRef().getReferanse()
        ).collect(Collectors.toSet());
    }


    private LocalDateTimeline<Boolean> finnOppfylteVilkårsperioderForrigeBehandlingTidslinje(BehandlingReferanse ref, Set<PeriodeTilVurdering> perioderTilVurdering) {
        return vilkårResultatRepository.hentHvisEksisterer(ref.getOriginalBehandlingId().orElseThrow()).orElseThrow()
            .getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR)
            .stream()
            .flatMap(v -> v.getPerioder().stream())
            .filter(p -> p.getGjeldendeUtfall().equals(Utfall.OPPFYLT))
            .map(VilkårPeriode::getPeriode)
            .map(p -> finnPeriodeIDenneBehandlingen(perioderTilVurdering, p))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(it -> new LocalDateTimeline<>(it.getFomDato(), it.getTomDato(), true))
            .reduce(LocalDateTimeline.empty(), (t1, t2) -> t1.crossJoin(t2, StandardCombinators::alwaysTrueForMatch));
    }

    private static Optional<DatoIntervallEntitet> finnPeriodeIDenneBehandlingen(Set<PeriodeTilVurdering> perioderTilVurdering, DatoIntervallEntitet p) {
        return perioderTilVurdering.stream().filter(it -> it.getSkjæringstidspunkt().equals(p.getFomDato())).map(PeriodeTilVurdering::getPeriode).findFirst();
    }


}
