package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningResultat;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.AktørArbeid;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.Hjelpetidslinjer;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.perioder.VurderSøknadsfristTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.PeriodeFraSøknadForBrukerTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.PerioderMedSykdomInnvilgetUtleder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.død.HåndterePleietrengendeDødsfallTjeneste;

@ApplicationScoped
public class ArbeidBrukerBurdeSøktOmUtleder {


    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;
    private Instance<VurderSøknadsfristTjeneste<Søknadsperiode>> søknadsfristTjenester;
    private Instance<HåndterePleietrengendeDødsfallTjeneste> håndterePleietrengendeDødsfallTjenester;
    private PeriodeFraSøknadForBrukerTjeneste periodeFraSøknadForBrukerTjeneste;
    private PerioderMedSykdomInnvilgetUtleder perioderMedSykdomInnvilgetUtleder;
    private OpptjeningRepository opptjeningRepository;
    private VilkårResultatRepository vilkårResultatRepository;

    public ArbeidBrukerBurdeSøktOmUtleder() {
    }

    @Inject
    public ArbeidBrukerBurdeSøktOmUtleder(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                          @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester,
                                          @Any Instance<VurderSøknadsfristTjeneste<Søknadsperiode>> søknadsfristTjenester,
                                          PeriodeFraSøknadForBrukerTjeneste periodeFraSøknadForBrukerTjeneste,
                                          PerioderMedSykdomInnvilgetUtleder perioderMedSykdomInnvilgetUtleder,
                                          OpptjeningRepository opptjeningRepository,
                                          VilkårResultatRepository vilkårResultatRepository,
                                          @Any Instance<HåndterePleietrengendeDødsfallTjeneste> håndterePleietrengendeDødsfallTjenester) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
        this.periodeFraSøknadForBrukerTjeneste = periodeFraSøknadForBrukerTjeneste;
        this.perioderMedSykdomInnvilgetUtleder = perioderMedSykdomInnvilgetUtleder;
        this.opptjeningRepository = opptjeningRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.søknadsfristTjenester = søknadsfristTjenester;
        this.håndterePleietrengendeDødsfallTjenester = håndterePleietrengendeDødsfallTjenester;
    }

    public Map<AktivitetIdentifikator, LocalDateTimeline<Boolean>> utledMangler(BehandlingReferanse referanse) {
        var søknadsfristTjeneste = VurderSøknadsfristTjeneste.finnSøknadsfristTjeneste(søknadsfristTjenester, referanse.getFagsakYtelseType());

        var vurderteSøknadsperioder = søknadsfristTjeneste.vurderSøknadsfrist(referanse);
        var vilkårene = vilkårResultatRepository.hent(referanse.getBehandlingId());
        var perioderFraSøknader = periodeFraSøknadForBrukerTjeneste.hentPerioderFraSøknad(referanse);
        var perioderTilVurdering = finnSykdomsperioder(referanse);
        var opptjeningResultat = opptjeningRepository.finnOpptjening(referanse.getBehandlingId());

        var tidslinjeTilVurdering = utledTidslinjeTilVurdering(perioderTilVurdering, vilkårene.getVilkår(VilkårType.OPPTJENINGSVILKÅRET));

        var input = new ArbeidstidMappingInput()
            .medSaksnummer(referanse.getSaksnummer())
            .medKravDokumenter(vurderteSøknadsperioder.keySet())
            .medPerioderFraSøknader(perioderFraSøknader)
            .medTidslinjeTilVurdering(tidslinjeTilVurdering)
            .medOpptjeningsResultat(opptjeningResultat.orElse(null))
            .medVilkår(vilkårene.getVilkår(VilkårType.OPPTJENINGSVILKÅRET).orElseThrow());

        HåndterePleietrengendeDødsfallTjeneste håndterePleietrengendeDødsfallTjeneste = HåndterePleietrengendeDødsfallTjeneste.velgTjeneste(håndterePleietrengendeDødsfallTjenester, referanse);
        håndterePleietrengendeDødsfallTjeneste.utledUtvidetPeriodeForDødsfall(referanse).ifPresent(input::medAutomatiskUtvidelseVedDødsfall);

        var innvilgeteVilkårPerioder = perioderMedSykdomInnvilgetUtleder.utledInnvilgedePerioderTilVurdering(referanse);

        var innvilgedeSegmenter = innvilgeteVilkårPerioder.stream()
            .map(periode -> new LocalDateSegment<>(periode.getFomDato(), periode.getTomDato(), true))
            .collect(Collectors.toList());

        var timelineMedYtelse = new LocalDateTimeline<>(innvilgedeSegmenter);
        // Trekke fra perioder med avslag fra
        // - Opptjening
        var timelineMedInnvilgetYtelse = utledYtelse(vilkårene, timelineMedYtelse);

        var aktørArbeidFraRegister = inntektArbeidYtelseTjeneste.hentGrunnlag(referanse.getBehandlingId()).getAktørArbeidFraRegister(referanse.getAktørId());

        return utledFraInput(timelineMedYtelse, timelineMedInnvilgetYtelse, input, aktørArbeidFraRegister);
    }

    private LocalDateTimeline<Boolean> utledTidslinjeTilVurdering(NavigableSet<DatoIntervallEntitet> perioderTilVurdering, Optional<Vilkår> vilkår) {
        var tidslinje = new LocalDateTimeline<>(perioderTilVurdering.stream().map(it -> new LocalDateSegment<>(it.toLocalDateInterval(), true)).collect(Collectors.toList())).compress();
        if (vilkår.isEmpty()) {
            return tidslinje;
        }

        var opptjeningstidslinje = new LocalDateTimeline<>(vilkår.get().getPerioder().stream().map(VilkårPeriode::getPeriode).map(it -> new LocalDateSegment<>(it.toLocalDateInterval(), true)).collect(Collectors.toList()));

        return opptjeningstidslinje.combine(tidslinje, StandardCombinators::leftOnly, LocalDateTimeline.JoinStyle.LEFT_JOIN);
    }

    private LocalDateTimeline<Boolean> utledYtelse(Vilkårene vilkårene, LocalDateTimeline<Boolean> tidslinjeTilVurdering) {
        var timeline = new LocalDateTimeline<>(tidslinjeTilVurdering.stream()
            .map(it -> new LocalDateSegment<>(it.getLocalDateInterval(), true))
            .toList());

        // NB! Ikke legg til vilkår her uten å prate med en voksen først. (Nei, du regnes ikke som en voksen)
        var vilkår = Set.of(VilkårType.OPPTJENINGSVILKÅRET);

        for (VilkårType type : vilkår) {
            var innvilgeteSegmenter = vilkårene.getVilkår(type)
                .map(Vilkår::getPerioder)
                .orElse(List.of())
                .stream()
                .filter(it -> Utfall.OPPFYLT.equals(it.getGjeldendeUtfall()))
                .map(VilkårPeriode::getPeriode)
                .map(DatoIntervallEntitet::toLocalDateInterval)
                .filter(datoInterval -> tidslinjeTilVurdering.intersects(new LocalDateTimeline<>(datoInterval, true)))
                .map(it -> new LocalDateSegment<>(it, true))
                .collect(Collectors.toList());

            var vilkårTimeline = new LocalDateTimeline<>(innvilgeteSegmenter);

            timeline = timeline.intersection(vilkårTimeline);
        }

        return timeline;
    }

    Map<AktivitetIdentifikator, LocalDateTimeline<Boolean>> utledFraInput(LocalDateTimeline<Boolean> tidslinjeTilVurdering, LocalDateTimeline<Boolean> timelineMedYtelse, ArbeidstidMappingInput input, Optional<AktørArbeid> aktørArbeid) {
        if (tidslinjeTilVurdering.isEmpty()) {
            return Map.of();
        }

        var søktArbeid = new MapArbeid().mapTilRaw(input);
        return utledHvaSomBurdeVærtSøktOm(timelineMedYtelse, aktørArbeid, søktArbeid, input.getOpptjeningResultat());
    }

    private NavigableSet<DatoIntervallEntitet> finnSykdomsperioder(BehandlingReferanse referanse) {
        VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, referanse.getFagsakYtelseType(), referanse.getBehandlingType());

        var timeline = new LocalDateTimeline<Boolean>(List.of());
        var definerendeVilkår = perioderTilVurderingTjeneste.definerendeVilkår();

        for (VilkårType vilkårType : definerendeVilkår) {
            var perioder = perioderTilVurderingTjeneste.utled(referanse.getBehandlingId(), vilkårType);
            var periodeTidslinje = new LocalDateTimeline<>(perioder.stream().map(it -> new LocalDateSegment<>(it.getFomDato(), it.getTomDato(), true)).collect(Collectors.toList()));

            timeline = timeline.combine(periodeTidslinje, StandardCombinators::alwaysTrueForMatch, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }

        return TidslinjeUtil.tilDatoIntervallEntiteter(timeline.compress());
    }

    private HashMap<AktivitetIdentifikator, LocalDateTimeline<Boolean>> utledHvaSomBurdeVærtSøktOm(LocalDateTimeline<Boolean> tidslinjeTilVurdering,
                                                                                                   Optional<AktørArbeid> aktørArbeid,
                                                                                                   Map<AktivitetIdentifikator, LocalDateTimeline<WrappedArbeid>> søktArbeid,
                                                                                                   OpptjeningResultat opptjeningResultat) {
        var mellomregning = new HashMap<AktivitetIdentifikator, LocalDateTimeline<Boolean>>();

        aktørArbeid.map(AktørArbeid::hentAlleYrkesaktiviteter)
            .orElse(Collections.emptyList())
            .stream()
            .filter(it -> ArbeidType.AA_REGISTER_TYPER.contains(it.getArbeidType()))
            .forEach(yrkesaktivitet -> mapYrkesAktivitet(mellomregning, yrkesaktivitet));

        mapFrilansOgSelvstendigNæring(opptjeningResultat, mellomregning, tidslinjeTilVurdering);

        var helgeTidslinje = Hjelpetidslinjer.lagTidslinjeMedKunHelger(tidslinjeTilVurdering);

        var resultat = new HashMap<AktivitetIdentifikator, LocalDateTimeline<Boolean>>();
        // Sjekk mot hva det skulle vært søkt om
        for (Map.Entry<AktivitetIdentifikator, LocalDateTimeline<Boolean>> entry : mellomregning.entrySet()) {
            var søktOm = søktArbeid.getOrDefault(entry.getKey(), new LocalDateTimeline<>(List.of()));

            var skulleVærtSøktOm = entry.getValue().disjoint(søktOm);
            skulleVærtSøktOm = skulleVærtSøktOm.intersection(tidslinjeTilVurdering);
            skulleVærtSøktOm = skulleVærtSøktOm.disjoint(helgeTidslinje);

            resultat.put(entry.getKey(), skulleVærtSøktOm);
        }

        return resultat;
    }

    private void mapFrilansOgSelvstendigNæring(OpptjeningResultat opptjeningResultat, Map<AktivitetIdentifikator, LocalDateTimeline<Boolean>> mellomregning, LocalDateTimeline<Boolean> tidslinjeTilVurdering) {
        for (LocalDateSegment<Boolean> segment : tidslinjeTilVurdering.toSegments()) {
            var skjæringstidspunkt = segment.getFom();
            var opptjening = opptjeningResultat.finnOpptjening(skjæringstidspunkt);

            if (opptjening.isEmpty()) {
                return;
            }

            var erFrilans = opptjening.orElseThrow()
                .getOpptjeningAktivitet()
                .stream()
                .filter(it -> OpptjeningAktivitetType.FRILANS.equals(it.getAktivitetType()))
                .anyMatch(it -> erAktivVedSTP(skjæringstidspunkt, it.getTom()));
            if (erFrilans) {
                leggTilSegmentForType(mellomregning, segment, new AktivitetIdentifikator(UttakArbeidType.FRILANSER));
            }

            var erNæringsdrivende = opptjening.orElseThrow()
                .getOpptjeningAktivitet()
                .stream()
                .filter(it -> OpptjeningAktivitetType.NÆRING.equals(it.getAktivitetType()))
                .anyMatch(it -> DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusDays(1), skjæringstidspunkt.minusDays(1)).overlapper(it.getFom(), it.getTom()));

            if (erNæringsdrivende) {
                leggTilSegmentForType(mellomregning, segment, new AktivitetIdentifikator(UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE));
            }
        }
    }

    private boolean erAktivVedSTP(LocalDate skjæringstidspunkt, LocalDate aktivitetSlutt) {
        return Objects.equals(skjæringstidspunkt, aktivitetSlutt) || skjæringstidspunkt.isBefore(aktivitetSlutt);
    }

    private void leggTilSegmentForType(Map<AktivitetIdentifikator, LocalDateTimeline<Boolean>> mellomregning, LocalDateSegment<Boolean> segment, AktivitetIdentifikator aktivitetIdentifikator) {
        var timeline = mellomregning.getOrDefault(aktivitetIdentifikator, new LocalDateTimeline<>(List.of()));

        timeline = timeline.combine(segment, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);

        mellomregning.put(aktivitetIdentifikator, timeline);
    }

    private AktivitetIdentifikator utledIdentifikator(Yrkesaktivitet yrkesaktivitet) {
        return new AktivitetIdentifikator(UttakArbeidType.ARBEIDSTAKER, yrkesaktivitet.getArbeidsgiver(), null);
    }

    private void mapYrkesAktivitet(HashMap<AktivitetIdentifikator, LocalDateTimeline<Boolean>> resultat, Yrkesaktivitet yrkesaktivitet) {
        var key = utledIdentifikator(yrkesaktivitet);
        var arbeidsAktivTidslinje = resultat.getOrDefault(key, new LocalDateTimeline<>(List.of()));

        var segmenter = yrkesaktivitet.getAnsettelsesPeriode()
            .stream()
            .map(it -> new LocalDateSegment<>(it.getPeriode().toLocalDateInterval(), true))
            .collect(Collectors.toList());
        var segmenterMedAvtaltArbeidstidOver0Prosent = yrkesaktivitet.getAlleAktivitetsAvtaler()
            .stream()
            .filter(it -> Objects.nonNull(it.getProsentsats()))
            .filter(it -> !it.getProsentsats().erNulltall())
            .map(it -> new LocalDateSegment<>(it.getPeriode().toLocalDateInterval(), true))
            .collect(Collectors.toList());
        // Har ikke helt kontroll på aa-reg mtp overlapp her så better safe than sorry
        for (LocalDateSegment<Boolean> segment : segmenter) {
            var arbeidsforholdTidslinje = new LocalDateTimeline<>(List.of(segment));
            arbeidsAktivTidslinje = arbeidsAktivTidslinje.combine(arbeidsforholdTidslinje, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }
        var arbeidstidOver0Prosent = new LocalDateTimeline<Boolean>(List.of());
        for (LocalDateSegment<Boolean> segment : segmenterMedAvtaltArbeidstidOver0Prosent) {
            var arbeidsforholdTidslinje = new LocalDateTimeline<>(List.of(segment));
            arbeidstidOver0Prosent = arbeidstidOver0Prosent.combine(arbeidsforholdTidslinje, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }

        arbeidsAktivTidslinje = arbeidsAktivTidslinje.intersection(arbeidstidOver0Prosent);

        resultat.put(key, arbeidsAktivTidslinje.compress());
    }
}
