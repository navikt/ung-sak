package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.Instance;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.InntektsmeldingRelevantForVilkårsrevurdering;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.vilkår.PeriodeTilVurdering;
import no.nav.k9.sak.ytelse.beregning.grunnlag.KompletthetPeriode;

class SkalForlengeAktivitetstatus {

    private final HarEndretInntektsmeldingVurderer harEndretInntektsmeldingVurderer;

    SkalForlengeAktivitetstatus(Instance<InntektsmeldingRelevantForVilkårsrevurdering> inntektsmeldingRelevantForBeregningVilkårsvurdering) {
        this.harEndretInntektsmeldingVurderer = new HarEndretInntektsmeldingVurderer(
            getInntektsmeldingFilter(inntektsmeldingRelevantForBeregningVilkårsvurdering),
            SkalForlengeAktivitetstatus::finnEndringer);
    }

    NavigableSet<PeriodeTilVurdering> finnPerioderForForlengelseAvStatus(SkalForlengeStatusInput skalForlengeStatusInput) {
        var oppfyltePerioderForrigeBehandlingTidslinje = finnOppfylteVilkårsperioderForrigeBehandlingTidslinje(skalForlengeStatusInput);
        var forlengelserIOpptjeningTidslinje = finnForlengelserIOpptjeningTidslinje(skalForlengeStatusInput);
        var prosesstriggerTidslinje = finnProsesstriggerTidslinje(skalForlengeStatusInput);
        var forlengelserIBeregningTidslinje = finnForlengelserIBeregningTidslinje(skalForlengeStatusInput);
        var utenEndringIInntektsmeldingTidslinje = finnTidslinjeForInntektsmeldingUtenEndring(skalForlengeStatusInput);
        var ingenEndringIKompletthetTidslinje = finnTidslinjeForKompletthetUtenEndring(skalForlengeStatusInput);

        var intervaller = skalForlengeStatusInput.perioderTilVurderingIBeregning()
            .stream().map(PeriodeTilVurdering::getPeriode)
            .collect(Collectors.toSet());

        var forlengetStatusTidslinje = TidslinjeUtil.tilTidslinjeKomprimert(intervaller)
            .intersection(forlengelserIOpptjeningTidslinje)
            .intersection(oppfyltePerioderForrigeBehandlingTidslinje)
            .intersection(utenEndringIInntektsmeldingTidslinje)
            .intersection(ingenEndringIKompletthetTidslinje)
            .disjoint(forlengelserIBeregningTidslinje)
            .disjoint(prosesstriggerTidslinje);

        var forlengeletStatusPerioder = TidslinjeUtil.tilDatoIntervallEntiteter(forlengetStatusTidslinje);
        return skalForlengeStatusInput.perioderTilVurderingIBeregning().stream()
            .filter(p -> forlengeletStatusPerioder.stream().anyMatch(it -> it.overlapper(p.getPeriode())))
            .collect(Collectors.toCollection(TreeSet::new));
    }

    private LocalDateTimeline<Boolean> finnTidslinjeForKompletthetUtenEndring(SkalForlengeStatusInput skalForlengeStatusInput) {
        return skalForlengeStatusInput.perioderTilVurderingIBeregning()
            .stream()
            .map(PeriodeTilVurdering::getPeriode)
            .filter(p -> erKompletthetsvurderingLikForrigeVedtak(
                skalForlengeStatusInput.gjeldendeKompletthetsvurdering(),
                skalForlengeStatusInput.forrigeKompletthetsvurdering(),
                p.getFomDato()))
            .map(it -> new LocalDateTimeline<>(it.getFomDato(), it.getTomDato(), true))
            .reduce(LocalDateTimeline.empty(), (t1, t2) -> t1.crossJoin(t2, StandardCombinators::alwaysTrueForMatch));
    }

    private LocalDateTimeline<Boolean> finnTidslinjeForInntektsmeldingUtenEndring(SkalForlengeStatusInput skalForlengeStatusInput) {
        return skalForlengeStatusInput.perioderTilVurderingIBeregning()
            .stream()
            .map(PeriodeTilVurdering::getPeriode)
            .filter((p) -> erInntektsmeldingerLikForrigeVedtak(
                skalForlengeStatusInput.behandlingReferanse(),
                skalForlengeStatusInput.originalBehandlingReferanse(),
                p, skalForlengeStatusInput.inntektsmeldinger(), skalForlengeStatusInput.mottatteInntektsmeldinger()))
            .map(it -> new LocalDateTimeline<>(it.getFomDato(), it.getTomDato(), true))
            .reduce(LocalDateTimeline.empty(), (t1, t2) -> t1.crossJoin(t2, StandardCombinators::alwaysTrueForMatch));
    }

    private static LocalDateTimeline<Boolean> finnForlengelserIBeregningTidslinje(SkalForlengeStatusInput skalForlengeStatusInput) {
        return skalForlengeStatusInput.perioderTilVurderingIBeregning().stream().filter(PeriodeTilVurdering::erForlengelse)
            .map(PeriodeTilVurdering::getPeriode)
            .map(it -> new LocalDateTimeline<>(it.getFomDato(), it.getTomDato(), true))
            .reduce(LocalDateTimeline.empty(), (t1, t2) -> t1.crossJoin(t2, StandardCombinators::alwaysTrueForMatch));
    }

    private LocalDateTimeline<Boolean> finnForlengelserIOpptjeningTidslinje(SkalForlengeStatusInput skalForlengeStatusInput) {
        return skalForlengeStatusInput.perioderTilVurderingIOpptjening().stream()
            .filter(PeriodeTilVurdering::erForlengelse)
            .map(PeriodeTilVurdering::getPeriode)
            .map(it -> new LocalDateTimeline<>(it.getFomDato(), it.getTomDato(), true))
            .reduce(LocalDateTimeline.empty(), (t1, t2) -> t1.crossJoin(t2, StandardCombinators::alwaysTrueForMatch));
    }

    private LocalDateTimeline<Boolean> finnProsesstriggerTidslinje(SkalForlengeStatusInput skalForlengeStatusInput) {
        return skalForlengeStatusInput.perioderForRevurderingAvBeregningFraProsesstrigger().stream()
            .map(it -> new LocalDateTimeline<>(it.getFomDato(), it.getTomDato(), true))
            .reduce(LocalDateTimeline.empty(), (t1, t2) -> t1.crossJoin(t2, StandardCombinators::alwaysTrueForMatch));
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

    private boolean erInntektsmeldingerLikForrigeVedtak(BehandlingReferanse ref, BehandlingReferanse originalBehalingreferanse, DatoIntervallEntitet periode, Set<Inntektsmelding> inntektsmeldings, List<MottattDokument> mottatteInntektsmeldinger) {
        return harEndretInntektsmeldingVurderer.finnInntektsmeldingerMedRelevanteEndringerForPerioden(ref,
            originalBehalingreferanse,
            periode, inntektsmeldings,
            mottatteInntektsmeldinger
        ).isEmpty();
    }


    static Collection<Inntektsmelding> finnEndringer(Collection<Inntektsmelding> relevanteInntektsmeldinger, Collection<Inntektsmelding> relevanteInntektsmeldingerForrigeVedtak) {
        var unikeArbeidsforhold = finnUnikeArbeidsforholdIdentifikatorer(relevanteInntektsmeldinger);
        var unikeArbeidsforholdForrigeVedtak = finnUnikeArbeidsforholdIdentifikatorer(relevanteInntektsmeldingerForrigeVedtak);
        var nyeArbeidsforhold = unikeArbeidsforhold.stream().filter(it -> !unikeArbeidsforholdForrigeVedtak.contains(it)).collect(Collectors.toSet());
        return relevanteInntektsmeldinger.stream().filter(im -> nyeArbeidsforhold.contains(finnArbeidsforholdIdentifikator(im))).collect(Collectors.toSet());
    }
    private static Set<String> finnUnikeArbeidsforholdIdentifikatorer(Collection<Inntektsmelding> relevanteInntektsmeldinger) {
        return relevanteInntektsmeldinger.stream().map(
            im -> im.getArbeidsgiver().getIdentifikator() + im.getArbeidsforholdRef().getReferanse()
        ).collect(Collectors.toSet());
    }

    private static String finnArbeidsforholdIdentifikator(Inntektsmelding im) {
        return im.getArbeidsgiver().getIdentifikator() + im.getArbeidsforholdRef().getReferanse();
    }


    private LocalDateTimeline<Boolean> finnOppfylteVilkårsperioderForrigeBehandlingTidslinje(SkalForlengeStatusInput skalForlengeStatusInput) {
        return skalForlengeStatusInput.innvilgedePerioderForrigeBehandling().stream()
            .map(p -> finnPeriodeIDenneBehandlingen(skalForlengeStatusInput.perioderTilVurderingIBeregning(), p))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(it -> new LocalDateTimeline<>(it.getFomDato(), it.getTomDato(), true))
            .reduce(LocalDateTimeline.empty(), (t1, t2) -> t1.crossJoin(t2, StandardCombinators::alwaysTrueForMatch));
    }

    private static Optional<DatoIntervallEntitet> finnPeriodeIDenneBehandlingen(Set<PeriodeTilVurdering> perioderTilVurdering, DatoIntervallEntitet p) {
        return perioderTilVurdering.stream().filter(it -> it.getSkjæringstidspunkt().equals(p.getFomDato())).map(PeriodeTilVurdering::getPeriode).findFirst();
    }

    private static HarEndretInntektsmeldingVurderer.InntektsmeldingFilter getInntektsmeldingFilter(Instance<InntektsmeldingRelevantForVilkårsrevurdering> inntektsmeldingRelevantForBeregningVilkårsvurdering) {
        return (BehandlingReferanse referanse, Collection<Inntektsmelding> sakInntektsmeldinger, DatoIntervallEntitet vilkårsPeriode) ->
            InntektsmeldingRelevantForVilkårsrevurdering.finnTjeneste(inntektsmeldingRelevantForBeregningVilkårsvurdering, VilkårType.BEREGNINGSGRUNNLAGVILKÅR, referanse.getFagsakYtelseType()).orElseThrow().begrensInntektsmeldinger(referanse, sakInntektsmeldinger, vilkårsPeriode);
    }

    record SkalForlengeStatusInput(
        BehandlingReferanse behandlingReferanse,
        BehandlingReferanse originalBehandlingReferanse,
        Set<Inntektsmelding> inntektsmeldinger,
        List<MottattDokument> mottatteInntektsmeldinger,
        NavigableSet<DatoIntervallEntitet> perioderForRevurderingAvBeregningFraProsesstrigger,
        NavigableSet<PeriodeTilVurdering> perioderTilVurderingIOpptjening,
        NavigableSet<PeriodeTilVurdering> perioderTilVurderingIBeregning,
        NavigableSet<DatoIntervallEntitet> innvilgedePerioderForrigeBehandling,
        Set<KompletthetPeriode> gjeldendeKompletthetsvurdering,
        Set<KompletthetPeriode> forrigeKompletthetsvurdering
    ) {

    }

}
