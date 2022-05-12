package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.kodeverk.uttak.SøknadÅrsak;
import no.nav.k9.kodeverk.uttak.Tid;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.k9.sak.domene.opptjening.OpptjeningAktivitetPeriode;
import no.nav.k9.sak.domene.opptjening.VurderingsStatus;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Stillingsprosent;
import no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding.AktivitetTypeArbeidsgiver;
import no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding.OppgittFraværHolder;
import no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding.OppgittFraværVerdi;
import no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding.SamtidigKravStatus;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

public class MapOppgittFraværOgVilkårsResultat {

    private static final Logger log = LoggerFactory.getLogger(MapOppgittFraværOgVilkårsResultat.class);

    Map<Aktivitet, List<WrappedOppgittFraværPeriode>> utledPerioderMedUtfall(BehandlingReferanse ref, InntektArbeidYtelseGrunnlag iayGrunnlag, NavigableMap<DatoIntervallEntitet, List<OpptjeningAktivitetPeriode>> opptjeningAktivitetPerioder, Vilkårene vilkårene, DatoIntervallEntitet fagsakPeriode, Map<AktivitetTypeArbeidsgiver, LocalDateTimeline<OppgittFraværHolder>> fraværsPerioder) {
        var filter = new YrkesaktivitetFilter(iayGrunnlag.getArbeidsforholdInformasjon(), iayGrunnlag.getAktørArbeidFraRegister(ref.getAktørId()));

        Map<Aktivitet, LocalDateTimeline<WrappedOppgittFraværPeriode>> fraværsTidslinje = opprettFraværsTidslinje(fagsakPeriode, fraværsPerioder);
        Map<Aktivitet, LocalDateTimeline<WrappedOppgittFraværPeriode>> arbeidsforholdOgPermitertTidslinje = opprettPermitertTidslinje(filter);
        Map<Aktivitet, LocalDateTimeline<WrappedOppgittFraværPeriode>> egenNæringTidslinje = opprettEgenNæringTidslinje(opptjeningAktivitetPerioder, ref.getAktørId());
        Map<Aktivitet, LocalDateTimeline<WrappedOppgittFraværPeriode>> oppgittFrilansTidslinje = opprettOppgittFrilansTidslinje(opptjeningAktivitetPerioder);

        fraværsTidslinje = kombinerFraværOgArbeidsforholdsTidslinjer(fraværsTidslinje, arbeidsforholdOgPermitertTidslinje);
        fraværsTidslinje = kombinerFraværOgArbeidsforholdsTidslinjer(fraværsTidslinje, egenNæringTidslinje);
        fraværsTidslinje = kombinerFraværOgArbeidsforholdsTidslinjer(fraværsTidslinje, oppgittFrilansTidslinje);

        fraværsTidslinje = leggPåVurderingAvNyoppstartetArbeidsforholdHvorAktuelt(fraværsTidslinje, iayGrunnlag, ref.getAktørId());

        LocalDateTimeline<WrappedOppgittFraværPeriode> avslåtteVilkårTidslinje = opprettAvslåttInngangsVilkårTidslinje(vilkårene);

        return kombinerTidslinjene(fraværsTidslinje, avslåtteVilkårTidslinje);
    }

    private Map<Aktivitet, LocalDateTimeline<WrappedOppgittFraværPeriode>> kombinerFraværOgArbeidsforholdsTidslinjer(Map<Aktivitet, LocalDateTimeline<WrappedOppgittFraværPeriode>> fraværsTidslinje,
                                                                                                                     Map<Aktivitet, LocalDateTimeline<WrappedOppgittFraværPeriode>> arbeidsforholdOgPermitertTidslinje) {
        var result = new HashMap<Aktivitet, LocalDateTimeline<WrappedOppgittFraværPeriode>>();
        for (Aktivitet aktivitet : fraværsTidslinje.keySet()) {
            var arbeidsforholdSomMatcher = arbeidsforholdOgPermitertTidslinje.keySet()
                .stream()
                .filter(it -> it.matcher(aktivitet))
                .map(arbeidsforholdOgPermitertTidslinje::get)
                .toList();
            result.put(aktivitet, mergeTidslinjer(fraværsTidslinje.get(aktivitet), arbeidsforholdSomMatcher));
        }
        // Arbeidsforhold som har fravær, men ikke finnes i arbeidsforhold
        var fraværUtenArbeidsforhold = fraværsTidslinje.keySet()
            .stream()
            .filter(it -> arbeidsforholdOgPermitertTidslinje.keySet().stream().noneMatch(it::matcher))
            .collect(Collectors.toSet());

        var ikkeIArbeidTidslinje = List.of(new LocalDateTimeline<>(List.of(new LocalDateSegment<>(Tid.TIDENES_BEGYNNELSE, Tid.TIDENES_ENDE, new WrappedOppgittFraværPeriode(ArbeidStatus.IKKE_EKSISTERENDE)))));
        fraværUtenArbeidsforhold.forEach(af -> result.put(af, mergeTidslinjer(fraværsTidslinje.get(af), ikkeIArbeidTidslinje)));
        return result;
    }

    private Map<Aktivitet, LocalDateTimeline<WrappedOppgittFraværPeriode>> leggPåVurderingAvNyoppstartetArbeidsforholdHvorAktuelt(Map<Aktivitet, LocalDateTimeline<WrappedOppgittFraværPeriode>> fraværsTidslinje, InntektArbeidYtelseGrunnlag iayGrunnlag, AktørId aktørId) {

        Set<SøknadÅrsak> søknadsårsaker = fraværsTidslinje.values().stream()
            .flatMap(fraværTidslinje -> fraværTidslinje.stream())
            .filter(segment -> segment.getValue().getPeriode() != null)
            .map(segment -> segment.getValue().getPeriode().getSøknadÅrsak())
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        log.info("Søknadsårsaker {}", søknadsårsaker);
        boolean vilkåretErAktuelt = søknadsårsaker.contains(SøknadÅrsak.NYOPPSTARTET_HOS_ARBEIDSGIVER);
        if (!vilkåretErAktuelt) {
            //ikke vits å lage strukturer for å utlede noe
            return fraværsTidslinje;
        }

        Map<Arbeidsgiver, LocalDateTimeline<Boolean>> perioderNyoppstartet = new NyoppstartetUtleder().utledPerioderMedNyoppstartetArbeidsforhold(iayGrunnlag, aktørId);
        log.info("Perioder nyoppstartet: {}", perioderNyoppstartet.values());

        var resultat = new HashMap<Aktivitet, LocalDateTimeline<WrappedOppgittFraværPeriode>>();
        for (var entry : fraværsTidslinje.entrySet()) {
            Aktivitet aktivitetId = entry.getKey();
            LocalDateTimeline<WrappedOppgittFraværPeriode> fraværTidslinje = entry.getValue();
            if (aktivitetId.getArbeidsgiver() == null) {
                resultat.put(aktivitetId, fraværTidslinje);
                continue;
            }
            log.info("Fraværsperioder:{} ", fraværsTidslinje);
            //må vurdere perioder hvor det er søknad som gjelder, og søknadårsak er NYOPPSTARTET_HOS_ARBEIDSGIVER
            LocalDateTimeline<Boolean> fraværsperioderSomSkalVurderes = new LocalDateTimeline<>(
                fraværTidslinje.stream()
                    .filter(segment -> segment.getValue().getPeriode() != null
                        && segment.getValue().getPeriode().getSøknadÅrsak() == SøknadÅrsak.NYOPPSTARTET_HOS_ARBEIDSGIVER
                        && segment.getValue().getSamtidigeKrav().inntektsmeldingMedRefusjonskrav() != SamtidigKravStatus.KravStatus.FINNES)
                    .map(segment -> new LocalDateSegment<>(segment.getLocalDateInterval(), true))
                    .toList());

            log.info("Perioder som vurderes ifht nyoppstartet: {} ", fraværsperioderSomSkalVurderes);

            LocalDateTimeline<Boolean> periodeNyoppstartet = perioderNyoppstartet.getOrDefault(aktivitetId.getArbeidsgiver(), LocalDateTimeline.empty());
            var innvilgetTidslinje = fraværsperioderSomSkalVurderes.mapValue(v -> no.nav.k9.aarskvantum.kontrakter.Utfall.INNVILGET).intersection(periodeNyoppstartet);
            var avslåttTidslinje = fraværsperioderSomSkalVurderes.mapValue(v -> no.nav.k9.aarskvantum.kontrakter.Utfall.AVSLÅTT).disjoint(periodeNyoppstartet);
            var vurdertTidslinje = innvilgetTidslinje.crossJoin(avslåttTidslinje, StandardCombinators::coalesceLeftHandSide);
            var vurdertTidslinjeWofp = vurdertTidslinje.mapValue(WrappedOppgittFraværPeriode::new);
            var oppdatertFraværTidslinje = fraværTidslinje.crossJoin(vurdertTidslinjeWofp, this::mergePeriode);
            resultat.put(aktivitetId, oppdatertFraværTidslinje);
        }
        return resultat;
    }

    private LocalDateTimeline<WrappedOppgittFraværPeriode> mergeTidslinjer(LocalDateTimeline<WrappedOppgittFraværPeriode> wrappedOppgittFraværPeriodeLocalDateTimeline,
                                                                           List<LocalDateTimeline<WrappedOppgittFraværPeriode>> arbeidsforholdSomMatcher) {
        var tidslinje = wrappedOppgittFraværPeriodeLocalDateTimeline;
        var arbeidsforholdTidslinje = mergeTidslinjer(arbeidsforholdSomMatcher);
        tidslinje = tidslinje.combine(arbeidsforholdTidslinje, this::mergePeriode, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        return tidslinje.compress();
    }

    private LocalDateTimeline<WrappedOppgittFraværPeriode> mergeTidslinjer(List<LocalDateTimeline<WrappedOppgittFraværPeriode>> arbeidsforholdSomMatcher) {
        var tidslinje = new LocalDateTimeline<WrappedOppgittFraværPeriode>(List.of());
        for (LocalDateTimeline<WrappedOppgittFraværPeriode> oppgittFraværPeriodeLocalDateTimeline : arbeidsforholdSomMatcher) {
            tidslinje = tidslinje.combine(oppgittFraværPeriodeLocalDateTimeline, this::mergePeriode, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }
        return tidslinje.compress();
    }

    private Map<Aktivitet, LocalDateTimeline<WrappedOppgittFraværPeriode>> opprettPermitertTidslinje(YrkesaktivitetFilter filter) {
        var result = new HashMap<Aktivitet, LocalDateTimeline<WrappedOppgittFraværPeriode>>();

        filter.getYrkesaktiviteter().forEach(ya -> mapYaTilTidlinje(ya, result, filter));

        return result;
    }

    private Map<Aktivitet, LocalDateTimeline<WrappedOppgittFraværPeriode>> opprettEgenNæringTidslinje(NavigableMap<DatoIntervallEntitet, List<OpptjeningAktivitetPeriode>> opptjeningAktivitetPerioder, AktørId aktørId) {
        var result = new HashMap<Aktivitet, LocalDateTimeline<WrappedOppgittFraværPeriode>>();
        opptjeningAktivitetPerioder.forEach((vilkårperiode, opptjeningPerioder) -> {
            Map<Aktivitet, List<OpptjeningAktivitetPeriode>> perioderPerAktivitet = finnOpptjeningAktivitetPerioder(OpptjeningAktivitetType.NÆRING, UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, opptjeningPerioder, aktørId);

            perioderPerAktivitet.forEach(((aktivitet, aktivitetPerioder) -> {
                LocalDateTimeline<WrappedOppgittFraværPeriode> tidslinje = result.getOrDefault(aktivitet, new LocalDateTimeline<>(List.of()));
                LocalDateTimeline<WrappedOppgittFraværPeriode> tidslinjeNyPeriode = byggOpptjeningAktivitetTidslinje(aktivitetPerioder, vilkårperiode);
                tidslinje = tidslinje.combine(tidslinjeNyPeriode, this::mergePeriode, LocalDateTimeline.JoinStyle.CROSS_JOIN);
                result.put(aktivitet, tidslinje.compress());
            }));
        });
        return result;
    }

    private Map<Aktivitet, List<OpptjeningAktivitetPeriode>> finnOpptjeningAktivitetPerioder(OpptjeningAktivitetType opptjeningAktivitetType, UttakArbeidType uttakArbeidType, List<OpptjeningAktivitetPeriode> opptjeningPerioder, AktørId aktørId) {
        var perioderPerAktivitet = opptjeningPerioder.stream()
            .filter(akt -> akt.getOpptjeningAktivitetType().equals(opptjeningAktivitetType))
            .filter(akt -> List.of(VurderingsStatus.GODKJENT, VurderingsStatus.FERDIG_VURDERT_GODKJENT, VurderingsStatus.TIL_VURDERING).contains(akt.getVurderingsStatus()))
            .collect(Collectors.groupingBy(aktivitetPeriode -> tilAktivitet(aktivitetPeriode, uttakArbeidType, aktørId)));
        return perioderPerAktivitet;
    }

    private Aktivitet tilAktivitet(OpptjeningAktivitetPeriode aktivitetPeriode, UttakArbeidType uttakArbeidType, AktørId aktørId) {
        Arbeidsgiver arbeidsgiver;

        if (aktivitetPeriode.getOpptjeningsnøkkel() != null) {
            arbeidsgiver = aktivitetPeriode.getOpptjeningsnøkkel().harType(Opptjeningsnøkkel.Type.ORG_NUMMER)
                ? Arbeidsgiver.virksomhet(aktivitetPeriode.getOpptjeningsnøkkel().getVerdi())
                : aktørId != null ? Arbeidsgiver.fra(aktørId) : null;
        } else if (aktørId != null) {
            // Egen næring uten orgnr
            arbeidsgiver = Arbeidsgiver.fra(aktørId);
        } else {
            // Frilans
            arbeidsgiver = null;
        }
        return new Aktivitet(uttakArbeidType, arbeidsgiver, InternArbeidsforholdRef.nullRef());
    }

    private LocalDateTimeline<WrappedOppgittFraværPeriode> byggOpptjeningAktivitetTidslinje(List<OpptjeningAktivitetPeriode> opptjeningPerioder, DatoIntervallEntitet vilkårperiode) {
        LocalDateTimeline<WrappedOppgittFraværPeriode> allVerdenAvTid = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(Tid.TIDENES_BEGYNNELSE, Tid.TIDENES_ENDE, new WrappedOppgittFraværPeriode(ArbeidStatus.AVSLUTTET))));
        var aktivPerioder = opptjeningPerioder.stream()
            .map(it -> new LocalDateSegment<>(vilkårperiode.getFomDato(), vilkårperiode.getTomDato(), new WrappedOppgittFraværPeriode(ArbeidStatus.AKTIVT)))
            .collect(Collectors.toList());

        LocalDateTimeline<WrappedOppgittFraværPeriode> aktivAktivitetTidslinje = allVerdenAvTid;
        for (LocalDateSegment<WrappedOppgittFraværPeriode> segment : aktivPerioder) {
            aktivAktivitetTidslinje = aktivAktivitetTidslinje.combine(new LocalDateTimeline<>(List.of(segment)), this::mergePeriode, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }
        return aktivAktivitetTidslinje;
    }

    private Map<Aktivitet, LocalDateTimeline<WrappedOppgittFraværPeriode>> opprettOppgittFrilansTidslinje(NavigableMap<DatoIntervallEntitet, List<OpptjeningAktivitetPeriode>> opptjeningAktivitetPerioder) {
        var result = new HashMap<Aktivitet, LocalDateTimeline<WrappedOppgittFraværPeriode>>();
        opptjeningAktivitetPerioder.forEach((vilkårperiode, opptjeningPerioder) -> {
            Map<Aktivitet, List<OpptjeningAktivitetPeriode>> perioderPerAktivitet = finnOpptjeningAktivitetPerioder(OpptjeningAktivitetType.FRILANS, UttakArbeidType.FRILANSER, opptjeningPerioder, null);

            perioderPerAktivitet.forEach(((aktivitet, aktivitetPerioder) -> {
                LocalDateTimeline<WrappedOppgittFraværPeriode> tidslinje = result.getOrDefault(aktivitet, new LocalDateTimeline<>(List.of()));
                LocalDateTimeline<WrappedOppgittFraværPeriode> tidslinjeNyPeriode = byggOpptjeningAktivitetTidslinje(aktivitetPerioder, vilkårperiode);
                tidslinje = tidslinje.combine(tidslinjeNyPeriode, this::mergePeriode, LocalDateTimeline.JoinStyle.CROSS_JOIN);
                result.put(aktivitet, tidslinje.compress());
            }));
        });
        return result;
    }

    private void mapYaTilTidlinje(Yrkesaktivitet yrkesaktivitet, HashMap<Aktivitet, LocalDateTimeline<WrappedOppgittFraværPeriode>> result, YrkesaktivitetFilter filter) {
        var tidlinje = opprettArbeidsforholdTidslinje(yrkesaktivitet, filter);
        result.put(new Aktivitet(UttakArbeidType.ARBEIDSTAKER, yrkesaktivitet.getArbeidsgiver(), yrkesaktivitet.getArbeidsforholdRef()), tidlinje.compress());
    }


    private LocalDateTimeline<WrappedOppgittFraværPeriode> opprettArbeidsforholdTidslinje(Yrkesaktivitet yrkesaktivitet, YrkesaktivitetFilter filter) {
        LocalDateTimeline<WrappedOppgittFraværPeriode> allVerdenAvTid = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(Tid.TIDENES_BEGYNNELSE, Tid.TIDENES_ENDE, new WrappedOppgittFraværPeriode(ArbeidStatus.AVSLUTTET))));
        var ansettelsesPerioder = filter.getAnsettelsesPerioder(yrkesaktivitet).stream()
            .map(it -> new LocalDateSegment<>(it.getPeriode().getFomDato(), it.getPeriode().getTomDato(), new WrappedOppgittFraværPeriode(ArbeidStatus.AKTIVT)))
            .collect(Collectors.toList());
        var permisjonsPerioder = yrkesaktivitet.getPermisjon()
            .stream()
            .filter(it -> erStørreEllerLik100Prosent(it.getProsentsats()))
            .map(it -> new LocalDateSegment<>(it.getFraOgMed(), it.getTilOgMed(), new WrappedOppgittFraværPeriode(null, null, true, null, null, null, null)))
            .collect(Collectors.toList());

        LocalDateTimeline<WrappedOppgittFraværPeriode> arbeidsforholdTidslinje = allVerdenAvTid;
        for (LocalDateSegment<WrappedOppgittFraværPeriode> segment : ansettelsesPerioder) {
            arbeidsforholdTidslinje = arbeidsforholdTidslinje.combine(new LocalDateTimeline<>(List.of(segment)), this::mergePeriode, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }
        for (LocalDateSegment<WrappedOppgittFraværPeriode> segment : permisjonsPerioder) {
            arbeidsforholdTidslinje = arbeidsforholdTidslinje.combine(new LocalDateTimeline<>(List.of(segment)), this::mergePeriode, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }
        return arbeidsforholdTidslinje;
    }

    private boolean erStørreEllerLik100Prosent(Stillingsprosent prosentsats) {
        return Stillingsprosent.HUNDRED.getVerdi().intValue() <= prosentsats.getVerdi().intValue();
    }

    private Map<Aktivitet, List<WrappedOppgittFraværPeriode>> kombinerTidslinjene(Map<Aktivitet, LocalDateTimeline<WrappedOppgittFraværPeriode>> fraværsTidslinje,
                                                                                  LocalDateTimeline<WrappedOppgittFraværPeriode> avslåtteVilkårTidslinje) {
        Map<Aktivitet, List<WrappedOppgittFraværPeriode>> result = new HashMap<>();

        for (Map.Entry<Aktivitet, LocalDateTimeline<WrappedOppgittFraværPeriode>> entry : fraværsTidslinje.entrySet()) {
            var timeline = entry.getValue().combine(avslåtteVilkårTidslinje, this::mergePeriode, LocalDateTimeline.JoinStyle.CROSS_JOIN).compress();

            result.put(entry.getKey(), timeline.map(this::splittDelevisFravær)
                .toSegments()
                .stream()
                .filter(it -> it.getValue() != null)
                .filter(it -> it.getValue().getPeriode() != null)
                .map(this::opprettHoldKonsistens)
                .collect(Collectors.toList()));
        }

        return result;
    }

    private List<LocalDateSegment<WrappedOppgittFraværPeriode>> splittDelevisFravær(LocalDateSegment<WrappedOppgittFraværPeriode> segment) {
        if (segment.getValue() == null) {
            return List.of();
        }
        if (segment.getValue().getPeriode() == null) {
            return List.of();
        }
        if (segment.getValue().getPeriode().getFraværPerDag() == null) {
            return List.of(segment);
        }
        var segmenter = new ArrayList<LocalDateSegment<WrappedOppgittFraværPeriode>>();
        var startDato = segment.getFom();
        var endDato = segment.getTom();
        segmenter.add(new LocalDateSegment<>(startDato, startDato, segment.getValue()));
        while (!startDato.equals(endDato)) {
            startDato = startDato.plusDays(1);
            segmenter.add(new LocalDateSegment<>(startDato, startDato, segment.getValue()));
        }

        return segmenter;
    }

    private LocalDateTimeline<WrappedOppgittFraværPeriode> opprettAvslåttInngangsVilkårTidslinje(Vilkårene vilkårene) {
        var avslåtteVilkårsPerioder = vilkårene.getVilkårene()
            .stream()
            .filter(it -> !VilkårType.SØKNADSFRIST.equals(it.getVilkårType()))
            .map(Vilkår::getPerioder)
            .flatMap(Collection::stream)
            .filter(it -> Utfall.IKKE_OPPFYLT.equals(it.getGjeldendeUtfall()))
            .map(it -> new LocalDateSegment<>(it.getFom(), it.getTom(), new WrappedOppgittFraværPeriode(null, null, null, null, true, null, null)))
            .collect(Collectors.toList());
        LocalDateTimeline<WrappedOppgittFraværPeriode> avslåtteVilkårTidslinje = new LocalDateTimeline<>(List.of());
        for (LocalDateSegment<WrappedOppgittFraværPeriode> segment : avslåtteVilkårsPerioder) {
            avslåtteVilkårTidslinje = avslåtteVilkårTidslinje.combine(new LocalDateTimeline<>(List.of(segment)), this::mergePeriode, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }
        return avslåtteVilkårTidslinje;
    }

    private Map<Aktivitet, LocalDateTimeline<WrappedOppgittFraværPeriode>> opprettFraværsTidslinje(DatoIntervallEntitet fagsakPeriode, Map<AktivitetTypeArbeidsgiver, LocalDateTimeline<OppgittFraværHolder>> perioder) {
        perioder = begrensTil(perioder, fagsakPeriode);

        List<WrappedOppgittFraværPeriode> wrappedOppgittFraværPeriodeListe = mapTilWrappedOppgittFravær(perioder);
        Map<Aktivitet, List<WrappedOppgittFraværPeriode>> perioderPrAktivitet = wrappedOppgittFraværPeriodeListe.stream()
            .collect(Collectors.groupingBy(WrappedOppgittFraværPeriode::getAktivitet));
        return perioderPrAktivitet.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> new LocalDateTimeline<>(e.getValue().stream().map(wofp -> new LocalDateSegment<>(wofp.getPeriode().getFom(), wofp.getPeriode().getTom(), wofp)).toList())));
    }

    private Map<AktivitetTypeArbeidsgiver, LocalDateTimeline<OppgittFraværHolder>> begrensTil(Map<AktivitetTypeArbeidsgiver, LocalDateTimeline<OppgittFraværHolder>> perioder, DatoIntervallEntitet fagsakPeriode) {
        //fraværsperioder utenfor fagsakperiode har oppstått før det ble innført en sperre mot at IM kan ha fraværsperiode for mer enn ett kalenderår
        LocalDateTimeline<Void> tidslinjeFagsakPeriode = new LocalDateTimeline<>(fagsakPeriode.toLocalDateInterval(), null);

        var resultat = new LinkedHashMap<AktivitetTypeArbeidsgiver, LocalDateTimeline<OppgittFraværHolder>>();
        for (var entry : perioder.entrySet()) {
            LocalDateTimeline<Void> fraværUtenforFagsakPerioder = entry.getValue().disjoint(tidslinjeFagsakPeriode).mapValue(v -> null);
            if (fraværUtenforFagsakPerioder.isEmpty()) {
                resultat.put(entry.getKey(), entry.getValue());
            } else {
                log.warn("Fravær perioder {} utenfor fagsak-periode {} ble filtert bort", fraværUtenforFagsakPerioder, fagsakPeriode);
                LocalDateTimeline<OppgittFraværHolder> tidlinjeInnenforFagsakperiode = entry.getValue().intersection(tidslinjeFagsakPeriode);
                resultat.put(entry.getKey(), tidlinjeInnenforFagsakperiode);
            }
        }
        return resultat;
    }

    private List<WrappedOppgittFraværPeriode> mapTilWrappedOppgittFravær(Map<AktivitetTypeArbeidsgiver, LocalDateTimeline<OppgittFraværHolder>> perioder) {
        return perioder.entrySet().stream()
            .flatMap(e -> mapTilWrappedOppgittFraværPeriode(e.getKey(), e.getValue()).stream())
            .toList();
    }

    private static List<WrappedOppgittFraværPeriode> mapTilWrappedOppgittFraværPeriode(AktivitetTypeArbeidsgiver aktivitetTypeArbeidsgiver, LocalDateTimeline<OppgittFraværHolder> oppgittFraværSegment) {
        return oppgittFraværSegment.stream()
            .flatMap(segment -> mapTilWrappedOppgittFraværPeriode(aktivitetTypeArbeidsgiver, segment).stream())
            .toList();
    }

    private static List<WrappedOppgittFraværPeriode> mapTilWrappedOppgittFraværPeriode(AktivitetTypeArbeidsgiver aktivitetTypeArbeidsgiver, LocalDateSegment<OppgittFraværHolder> oppgittFraværSegment) {
        JournalpostId journalpostId = null;

        List<WrappedOppgittFraværPeriode> resultat = new ArrayList<>();
        LocalDate fom = oppgittFraværSegment.getFom();
        LocalDate tom = oppgittFraværSegment.getTom();

        OppgittFraværHolder oppgittFraværHolder = oppgittFraværSegment.getValue();
        SamtidigKravStatus samtidigKravStatus = oppgittFraværHolder.samtidigKravStatus();
        if (oppgittFraværHolder.refusjonskravGjelder()) {
            oppgittFraværHolder.getRefusjonskrav().forEach(
                (arbeidsforholdRef, oppgittFraværVerdi) -> {
                    OppgittFraværPeriode oppgittFraværPeriode = new OppgittFraværPeriode(journalpostId, fom, tom, aktivitetTypeArbeidsgiver.aktivitetType(), aktivitetTypeArbeidsgiver.arbeidsgiver(), arbeidsforholdRef, oppgittFraværVerdi.fraværPerDag(), oppgittFraværHolder.fraværÅrsak(), oppgittFraværHolder.søknadÅrsak());
                    var wofp = new WrappedOppgittFraværPeriode(oppgittFraværPeriode, oppgittFraværVerdi.innsendingstidspunkt(), null, null, utledInngangsvilkårUtfallSøknadsfrist(oppgittFraværVerdi.fraværPerDag(), oppgittFraværVerdi.søknadsfristUtfall()), samtidigKravStatus, null);
                    resultat.add(wofp);
                });
        } else if (oppgittFraværHolder.søknadGjelder()) {
            OppgittFraværVerdi oppgittFraværVerdi = oppgittFraværHolder.getSøknad();
            var arbeidsforholdRef = InternArbeidsforholdRef.nullRef();
            OppgittFraværPeriode oppgittFraværPeriode = new OppgittFraværPeriode(journalpostId, fom, tom, aktivitetTypeArbeidsgiver.aktivitetType(), aktivitetTypeArbeidsgiver.arbeidsgiver(), arbeidsforholdRef, oppgittFraværVerdi.fraværPerDag(), oppgittFraværHolder.fraværÅrsak(), oppgittFraværHolder.søknadÅrsak());
            var wofp = new WrappedOppgittFraværPeriode(oppgittFraværPeriode, oppgittFraværVerdi.innsendingstidspunkt(), null, null, utledInngangsvilkårUtfallSøknadsfrist(oppgittFraværVerdi.fraværPerDag(), oppgittFraværVerdi.søknadsfristUtfall()), samtidigKravStatus, null);
            resultat.add(wofp);
        }
        return resultat;
    }


    private static Boolean utledInngangsvilkårUtfallSøknadsfrist(Duration fraværPrDag, Utfall utfall) {
        if (Duration.ZERO.equals(fraværPrDag)) {
            return null;
        }
        return Utfall.IKKE_OPPFYLT.equals(utfall) ? true : null;
    }

    private WrappedOppgittFraværPeriode opprettHoldKonsistens(LocalDateSegment<WrappedOppgittFraværPeriode> segment) {
        var segmentValue = segment.getValue();
        var oppgittPeriode = segmentValue.getPeriode();
        return new WrappedOppgittFraværPeriode(new OppgittFraværPeriode(oppgittPeriode.getJournalpostId(), segment.getFom(), segment.getTom(), oppgittPeriode.getAktivitetType(),
            oppgittPeriode.getArbeidsgiver(), oppgittPeriode.getArbeidsforholdRef(), oppgittPeriode.getFraværPerDag(), oppgittPeriode.getFraværÅrsak(), oppgittPeriode.getSøknadÅrsak()), segmentValue.getInnsendingstidspunkt(), segmentValue.getErIPermisjon(), segmentValue.getArbeidStatus(), segmentValue.getErAvslåttInngangsvilkår(), segmentValue.getSamtidigeKrav(), segmentValue.getUtfallNyoppstartetVilkår());
    }

    private LocalDateSegment<WrappedOppgittFraværPeriode> mergePeriode(LocalDateInterval di,
                                                                       LocalDateSegment<WrappedOppgittFraværPeriode> førsteVersjon,
                                                                       LocalDateSegment<WrappedOppgittFraværPeriode> sisteVersjon) {
        if (førsteVersjon == null && sisteVersjon != null) {
            return lagSegment(di, sisteVersjon.getValue());
        } else if (sisteVersjon == null && førsteVersjon != null) {
            return lagSegment(di, førsteVersjon.getValue());
        }
        var første = førsteVersjon.getValue();
        var siste = sisteVersjon.getValue();

        var avslåttInngangsvilkår = booleanPrioTrue(første.getErAvslåttInngangsvilkår(), siste.getErAvslåttInngangsvilkår());
        var iPermisjon = booleanPrioTrue(første.getErIPermisjon(), siste.getErIPermisjon());
        var ikkeIArbeid = arbeidStatusPrioAktivt(første.getArbeidStatus(), siste.getArbeidStatus());
        var innsendingstidspunkt = utledInnsendingstidspunkt(første.getInnsendingstidspunkt(), siste.getInnsendingstidspunkt());
        var kravStatus = utledKravStatus(første.getSamtidigeKrav(), siste.getSamtidigeKrav());
        no.nav.k9.aarskvantum.kontrakter.Utfall utfallNyoppstartet = utledNyoppstartetUtfall(første.getUtfallNyoppstartetVilkår(), siste.getUtfallNyoppstartetVilkår());

        return lagSegment(di, avslåttInngangsvilkår, utfallNyoppstartet, utledOppgittPeriode(første.getPeriode(), siste.getPeriode()), iPermisjon, ikkeIArbeid, innsendingstidspunkt, kravStatus);
    }

    private SamtidigKravStatus utledKravStatus(SamtidigKravStatus førsteKravStatus, SamtidigKravStatus sisteKravStatus) {
        if (førsteKravStatus == null) {
            return sisteKravStatus;
        }
        if (sisteKravStatus == null) {
            return førsteKravStatus;
        }
        if (!Objects.equals(førsteKravStatus, sisteKravStatus)) {
            throw new IllegalArgumentException("Forventer ikke å slå sammen ulike krav-statuser her.");
        }
        return sisteKravStatus;
    }

    private no.nav.k9.aarskvantum.kontrakter.Utfall utledNyoppstartetUtfall(no.nav.k9.aarskvantum.kontrakter.Utfall første, no.nav.k9.aarskvantum.kontrakter.Utfall siste) {
        if (første == null) {
            return siste;
        }
        if (siste == null) {
            return første;
        }
        if (!Objects.equals(første, siste)) {
            throw new IllegalArgumentException("Forventer ikke å slå sammen ulike Utfall her.");
        }
        return siste;
    }


    private LocalDateTime utledInnsendingstidspunkt(LocalDateTime innsendingstidspunkt, LocalDateTime sisteInnsendingstidspunkt) {
        if (innsendingstidspunkt == null) {
            return sisteInnsendingstidspunkt;
        }
        if (sisteInnsendingstidspunkt == null) {
            return innsendingstidspunkt;
        }
        if (innsendingstidspunkt.isBefore(sisteInnsendingstidspunkt)) {
            return sisteInnsendingstidspunkt;
        }
        return innsendingstidspunkt;
    }

    private ArbeidStatus arbeidStatusPrioAktivt(ArbeidStatus status1, ArbeidStatus status2) {
        if (status1 == null || status2 == null) {
            return status2 != null ? status2 : status1;
        }
        if (status1.equals(status2)) {
            return status1;
        }
        if (ArbeidStatus.AKTIVT.equals(status1) || ArbeidStatus.AKTIVT.equals(status2)) {
            return ArbeidStatus.AKTIVT;
        }
        if (ArbeidStatus.AVSLUTTET.equals(status1) || ArbeidStatus.AVSLUTTET.equals(status2)) {
            return ArbeidStatus.AVSLUTTET;
        }
        return status1;
    }

    private Boolean booleanPrioTrue(Boolean boolOne, Boolean boolTwo) {
        if (boolOne == null || boolTwo == null) {
            return boolTwo != null ? boolTwo : boolOne;
        }
        return boolOne || boolTwo;
    }

    private OppgittFraværPeriode utledOppgittPeriode(OppgittFraværPeriode a, OppgittFraværPeriode b) {
        if (a == null) {
            return b;
        }
        return a;
    }

    private LocalDateSegment<WrappedOppgittFraværPeriode> lagSegment(LocalDateInterval di, Boolean erAvslått, no.nav.k9.aarskvantum.kontrakter.Utfall utfallNyoppstartet, OppgittFraværPeriode oppgittPeriode, Boolean iPermisjon, ArbeidStatus ikkeIArbeid, LocalDateTime innsendingstidspunkt, SamtidigKravStatus samtidigKravStatus) {
        var oppdaterOppgittFravær = oppgittPeriode != null
            ? new OppgittFraværPeriode(oppgittPeriode.getJournalpostId(), di.getFomDato(), di.getTomDato(), oppgittPeriode.getAktivitetType(), oppgittPeriode.getArbeidsgiver(), oppgittPeriode.getArbeidsforholdRef(), oppgittPeriode.getFraværPerDag(), oppgittPeriode.getFraværÅrsak(), oppgittPeriode.getSøknadÅrsak())
            : null;
        var wrapper = new WrappedOppgittFraværPeriode(oppdaterOppgittFravær, innsendingstidspunkt, iPermisjon, ikkeIArbeid, erAvslått, samtidigKravStatus, utfallNyoppstartet);
        return new LocalDateSegment<>(di, wrapper);
    }

    private LocalDateSegment<WrappedOppgittFraværPeriode> lagSegment(LocalDateInterval di, WrappedOppgittFraværPeriode segmentValue) {
        var oppgittPeriode = segmentValue.getPeriode();
        var oppdaterOppgittFravær = oppgittPeriode != null
            ? new OppgittFraværPeriode(oppgittPeriode.getJournalpostId(), di.getFomDato(), di.getTomDato(),
            oppgittPeriode.getAktivitetType(), oppgittPeriode.getArbeidsgiver(), oppgittPeriode.getArbeidsforholdRef(),
            oppgittPeriode.getFraværPerDag(), oppgittPeriode.getFraværÅrsak(), oppgittPeriode.getSøknadÅrsak())
            : null;
        var wrapper = new WrappedOppgittFraværPeriode(oppdaterOppgittFravær, segmentValue.getInnsendingstidspunkt(), segmentValue.getErIPermisjon(), segmentValue.getArbeidStatus(), segmentValue.getErAvslåttInngangsvilkår(), segmentValue.getSamtidigeKrav(), segmentValue.getUtfallNyoppstartetVilkår());
        return new LocalDateSegment<>(di, wrapper);
    }
}
