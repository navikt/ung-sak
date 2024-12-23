package no.nav.ung.sak.perioder;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.KantIKantVurderer;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.domene.typer.tid.Hjelpetidslinjer;
import no.nav.ung.sak.kontrakt.krav.KravDokumentMedSøktePerioder;
import no.nav.ung.sak.kontrakt.krav.KravDokumentType;
import no.nav.ung.sak.kontrakt.krav.PeriodeMedÅrsaker;
import no.nav.ung.sak.kontrakt.krav.PerioderMedÅrsakPerKravstiller;
import no.nav.ung.sak.kontrakt.krav.RolleType;
import no.nav.ung.sak.kontrakt.krav.StatusForPerioderPåBehandling;
import no.nav.ung.sak.kontrakt.krav.ÅrsakMedPerioder;
import no.nav.ung.sak.kontrakt.krav.ÅrsakTilVurdering;
import no.nav.ung.sak.typer.Arbeidsgiver;
import no.nav.ung.sak.typer.Periode;

public class UtledStatusPåPerioderTjeneste {

    private Boolean filtrereUtTilstøtendePeriode;

    private UtledPerioderMedRegisterendring utledPerioderMedRegisterendring;

    public UtledStatusPåPerioderTjeneste(Boolean filtrereUtTilstøtendePeriode,
                                         UtledPerioderMedRegisterendring utledPerioderMedRegisterendring) {
        this.filtrereUtTilstøtendePeriode = filtrereUtTilstøtendePeriode;
        this.utledPerioderMedRegisterendring = utledPerioderMedRegisterendring;
    }

    private static Map<KravDokument, List<SøktPeriode<VurdertSøktPeriode.SøktPeriodeData>>> alleKravdokumenterForArbeidsgiver(Map<KravDokument, List<SøktPeriode<VurdertSøktPeriode.SøktPeriodeData>>> alleKravdokumenterMedPeriode, Arbeidsgiver arbeidsgiver) {
        return alleKravdokumenterMedPeriode.entrySet()
            .stream()
            .filter(it -> it.getKey().getType() == no.nav.ung.sak.perioder.KravDokumentType.INNTEKTSMELDING)
            .filter(e -> e.getValue().stream().anyMatch(at -> arbeidsgiver.equals(at.getArbeidsgiver())))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public StatusForPerioderPåBehandling utled(Behandling behandling,
                                               KantIKantVurderer kantIKantVurderer,
                                               Set<KravDokument> kravdokumenter,
                                               Map<KravDokument, List<SøktPeriode<VurdertSøktPeriode.SøktPeriodeData>>> kravdokumenterMedPeriode,
                                               NavigableSet<DatoIntervallEntitet> perioderTilVurdering,
                                               NavigableSet<DatoIntervallEntitet> perioderSomSkalTilbakestilles) {


        List<PeriodeMedÅrsaker> perioder = perioderMedÅrsaker(behandling,
            kantIKantVurderer,
            kravdokumenter,
            kravdokumenterMedPeriode,
            perioderTilVurdering,
            perioderSomSkalTilbakestilles
        );


        var årsakMedPerioder = utledÅrsakMedPerioder(perioder);

        var perioderTilVurderingKombinert = new LocalDateTimeline<>(perioderTilVurdering.stream().map(it -> new LocalDateSegment<>(it.getFomDato(), it.getTomDato(), true)).collect(Collectors.toList()), StandardCombinators::alwaysTrueForMatch)
            .compress();

        var perioderTilVurderingSet = utledPerioderTilVurdering(perioderTilVurderingKombinert, årsakMedPerioder);

        Set<PerioderMedÅrsakPerKravstiller> perioderMedÅrsakPerKravstiller;
        if (behandling.getFagsakYtelseType() == FagsakYtelseType.OMP) {
            perioderMedÅrsakPerKravstiller = perioderMedÅrsakPerKravstiller(behandling,
                kantIKantVurderer,
                kravdokumenter,
                kravdokumenterMedPeriode,
                perioderTilVurdering,
                perioderSomSkalTilbakestilles,
                perioder);
        } else {
            perioderMedÅrsakPerKravstiller = Set.of(new PerioderMedÅrsakPerKravstiller(RolleType.BRUKER, null, perioder));
        }

        var relevanteDokumenterMedPeriode = utledKravdokumenterTilkommetIBehandlingen(kravdokumenter, kravdokumenterMedPeriode);

        return new StatusForPerioderPåBehandling(
            perioderTilVurderingSet,
            utledPerioderMedRegisterendring.utledPerioderMedRegisterendring(BehandlingReferanse.fra(behandling)),
            perioder,
            årsakMedPerioder,
            mapKravTilDto(relevanteDokumenterMedPeriode),
            perioderMedÅrsakPerKravstiller.stream().toList());
    }

    private Set<PerioderMedÅrsakPerKravstiller> perioderMedÅrsakPerKravstiller(
        Behandling behandling,
        KantIKantVurderer kantIKantVurderer,
        Set<KravDokument> kravdokumenter,
        Map<KravDokument, List<SøktPeriode<VurdertSøktPeriode.SøktPeriodeData>>> kravdokumenterMedPeriode,
        NavigableSet<DatoIntervallEntitet> perioderTilVurdering,
        NavigableSet<DatoIntervallEntitet> perioderSomSkalTilbakestilles,
        List<PeriodeMedÅrsaker> perioderMedÅrsakPåTversAvKravstillere) {

        Set<PerioderMedÅrsakPerKravstiller> resultat = new HashSet<>();

        if (kravdokumenter.stream().anyMatch(it -> it.getType() == no.nav.ung.sak.perioder.KravDokumentType.SØKNAD)) {
            resultat.add(new PerioderMedÅrsakPerKravstiller(RolleType.BRUKER, null,
                perioderMedÅrsakPåTversAvKravstillere));
        }

        var kravdokumenterPerKravstiller = kravdokumenter
            .stream()
            //Denne er nødvendig fordi type for kravdokumenter som gjelder for behandling ikke mappes til INNTEKTSMELDING_UTEN_REFUSJONSKRAV
            .filter(it -> !erInntektmeldingUtenRefusjonskrav(it, kravdokumenterMedPeriode))
            .filter(it -> it.getType() != no.nav.ung.sak.perioder.KravDokumentType.SØKNAD)
            .map(it -> mapTilKravdokumentPerRolle(it, kravdokumenterMedPeriode))
            .collect(Collectors.groupingBy(KravdokumenterPerKravstiller::rolleType));

        var ka = kravdokumenterPerKravstiller.get(RolleType.ARBEIDSGIVER);

        if (ka != null) {
            ka.stream()
                .collect(Collectors.groupingBy(KravdokumenterPerKravstiller::arbeidsgiver))
                .forEach((arbeidsgiver, kravdokumenterPerArbeidsgiver) ->
                    resultat.add(new PerioderMedÅrsakPerKravstiller(RolleType.ARBEIDSGIVER, arbeidsgiver,
                        perioderMedÅrsaker(behandling,
                            kantIKantVurderer,
                            kravdokumenterPerArbeidsgiver.stream()
                                .map(KravdokumenterPerKravstiller::kravdokumentForBehandling)
                                .collect(Collectors.toSet()),
                            alleKravdokumenterForArbeidsgiver(kravdokumenterMedPeriode, arbeidsgiver),
                            perioderTilVurdering,
                            perioderSomSkalTilbakestilles
                        ))
                    ));
        }

        return resultat;
    }

    private boolean erInntektmeldingUtenRefusjonskrav(KravDokument kravDokument, Map<KravDokument, List<SøktPeriode<VurdertSøktPeriode.SøktPeriodeData>>> kravdokumenterMedPeriode) {
        return kravdokumenterMedPeriode.keySet().stream()
            .filter(it -> it.getType() == no.nav.ung.sak.perioder.KravDokumentType.INNTEKTSMELDING_UTEN_REFUSJONSKRAV)
            .anyMatch(it -> kravDokument.getJournalpostId().equals(it.getJournalpostId()));
    }

    private KravdokumenterPerKravstiller mapTilKravdokumentPerRolle(
        KravDokument kravdokumentPåBehandling,
        Map<KravDokument, List<SøktPeriode<VurdertSøktPeriode.SøktPeriodeData>>> alleKravdokumenterMedPeriode) {

        RolleType rolleType = utledRolle(kravdokumentPåBehandling.getType());

        if (rolleType == RolleType.ARBEIDSGIVER) {

            Arbeidsgiver arbeidsgiver = alleKravdokumenterMedPeriode.entrySet()
                .stream()
                .filter(it -> it.getKey().getType() != no.nav.ung.sak.perioder.KravDokumentType.INNTEKTSMELDING_UTEN_REFUSJONSKRAV)
                .filter(it -> kravdokumentPåBehandling.getJournalpostId().equals(it.getKey().getJournalpostId()))
                .findAny().orElseThrow(() -> new IllegalArgumentException("Kravdokument mangler i alle kravdokument lista"))
                .getValue().stream()
                .findAny().orElseThrow(() -> new IllegalArgumentException("Mangler søknadsdata for kravdokument"))
                .getArbeidsgiver();

            return new KravdokumenterPerKravstiller(
                rolleType,
                arbeidsgiver,
                kravdokumentPåBehandling
            );
        }

        return new KravdokumenterPerKravstiller(
            rolleType,
            null,
            kravdokumentPåBehandling
        );
    }

    private RolleType utledRolle(no.nav.ung.sak.perioder.KravDokumentType type) {
        return type == no.nav.ung.sak.perioder.KravDokumentType.SØKNAD ? RolleType.BRUKER : RolleType.ARBEIDSGIVER;
    }

    private List<PeriodeMedÅrsaker> perioderMedÅrsaker(
        Behandling behandling,
        KantIKantVurderer kantIKantVurderer,
        Set<KravDokument> kravdokumenter,
        Map<KravDokument, List<SøktPeriode<VurdertSøktPeriode.SøktPeriodeData>>> kravdokumenterMedPeriode,
        NavigableSet<DatoIntervallEntitet> perioderTilVurdering,
        NavigableSet<DatoIntervallEntitet> perioderSomSkalTilbakestilles) {

        var relevanteDokumenterMedPeriode = utledKravdokumenterTilkommetIBehandlingen(kravdokumenter, kravdokumenterMedPeriode);

        var relevanteTidslinjer = relevanteDokumenterMedPeriode.stream()
            .map(entry -> tilSegments(entry, kantIKantVurderer, ÅrsakTilVurdering.FØRSTEGANGSVURDERING))
            .map(LocalDateTimeline::new)
            .toList();

        //tidslinjer med perioder fra dokumenter tilkommet i behandling
        var tidslinje = mergeTidslinjer(relevanteTidslinjer, kantIKantVurderer, this::mergeSegments);

        //perioder fra dokumenter som ikke tilhører denne behandlingen, men overlapper med perioderTilVurdering
        var andreRelevanteDokumenterForPeriodenTilVurdering = utledKravdokumenterRelevantForPeriodeTilVurdering(kravdokumenter, kravdokumenterMedPeriode, perioderTilVurdering);

        var endringFraBruker = andreRelevanteDokumenterForPeriodenTilVurdering.stream()
            .map(entry -> tilSegments(entry, kantIKantVurderer, utledRevurderingÅrsak(behandling)))
            .map(LocalDateTimeline::new)
            .toList();

        //tidslinje med perioder og årsaker hentet fra dokumenter som ikke er tilkommet denne behandlingen.
        var endringFraBrukerTidslinje = mergeTidslinjer(endringFraBruker, kantIKantVurderer, this::mergeSegmentsAndreDokumenter);

        tidslinje = tidslinje.combine(endringFraBrukerTidslinje, this::mergeSegmentsAndreDokumenter, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        tidslinje = tidslinje.filterValue(this::harIkkeBareBerørtPeriode);

        // En side effect av tidligere mergeAndreBerørtSaker som videreføres etter at andre parter ble fjernet
        tidslinje = velgFørstegangsbehandlingSomEnesteÅrsakHvisFinnes(tidslinje);


        var perioderTilVurderingKombinert = new LocalDateTimeline<>(perioderTilVurdering.stream().map(it -> new LocalDateSegment<>(it.getFomDato(), it.getTomDato(), true)).collect(Collectors.toList()), StandardCombinators::alwaysTrueForMatch)
            .compress();

        tidslinje = tidslinje.intersection(perioderTilVurderingKombinert);
        var tilbakestillingSegmenter = perioderSomSkalTilbakestilles.stream()
            .map(it -> new LocalDateSegment<>(it.getFomDato(), it.getTomDato(), new ÅrsakerTilVurdering(Set.of(ÅrsakTilVurdering.TRUKKET_KRAV))))
            .collect(Collectors.toList());

        tidslinje = tidslinje.combine(new LocalDateTimeline<>(tilbakestillingSegmenter, StandardCombinators::coalesceRightHandSide), this::mergeSegmentsAndreDokumenter, LocalDateTimeline.JoinStyle.CROSS_JOIN);

        var perioder = tidslinje.compress()
            .stream()
            .map(it -> new PeriodeMedÅrsaker(new Periode(it.getFom(), it.getTom()), transformerÅrsaker(it)))
            .collect(Collectors.toList());
        return perioder;
    }

    private static LocalDateTimeline<ÅrsakerTilVurdering> velgFørstegangsbehandlingSomEnesteÅrsakHvisFinnes(LocalDateTimeline<ÅrsakerTilVurdering> tidslinje) {
        return tidslinje.mapValue(v -> {
            var årsaker = v.getÅrsaker();
            if (v.getÅrsaker().contains(ÅrsakTilVurdering.FØRSTEGANGSVURDERING)) {
                if (årsaker.contains(ÅrsakTilVurdering.UTSATT_BEHANDLING)) {
                    årsaker = new HashSet<>(Set.of(ÅrsakTilVurdering.FØRSTEGANGSVURDERING, ÅrsakTilVurdering.UTSATT_BEHANDLING));
                } else {
                    årsaker = new HashSet<>(Set.of(ÅrsakTilVurdering.FØRSTEGANGSVURDERING));
                }
            }
            return new ÅrsakerTilVurdering(årsaker, v.getKravdokumenterForÅrsaker());
        });
    }

    private Set<Periode> utledPerioderTilVurdering(LocalDateTimeline<Boolean> perioderTilVurderingKombinert, List<ÅrsakMedPerioder> årsakMedPerioder) {
        if (!filtrereUtTilstøtendePeriode) {
            return perioderTilVurderingKombinert
                .stream()
                .map(it -> new Periode(it.getFom(), it.getTom()))
                .collect(Collectors.toCollection(TreeSet::new));
        }
        var segmenter = årsakMedPerioder.stream().map(ÅrsakMedPerioder::getPerioder).flatMap(Collection::stream).map(it -> new LocalDateSegment<>(it.getFom(), it.getTom(), true)).toList();
        var timeline = new LocalDateTimeline<>(segmenter, StandardCombinators::coalesceRightHandSide);
        return timeline.compress().stream().map(it -> new Periode(it.getFom(), it.getTom())).collect(Collectors.toCollection(TreeSet::new));
    }

    private boolean harIkkeBareBerørtPeriode(ÅrsakerTilVurdering it) {
        if (!filtrereUtTilstøtendePeriode) {
            return true;
        }
        var årsaker = it.getÅrsaker();
        return !årsaker.isEmpty() && !(årsaker.size() == 1 && årsaker.contains(ÅrsakTilVurdering.REVURDERER_BERØRT_PERIODE));
    }

    private List<ÅrsakMedPerioder> utledÅrsakMedPerioder(List<PeriodeMedÅrsaker> perioder) {
        var result = new HashMap<ÅrsakTilVurdering, LocalDateTimeline<Boolean>>();
        var årsakTilVurderingMap = perioder.stream().flatMap(this::tilÅrsakMedPerioder).collect(Collectors.groupingBy(ÅrsakMedPerioder::getÅrsak,
            Collectors.flatMapping(it -> Stream.of(it.getPerioder()).flatMap(Collection::stream), Collectors.toCollection(TreeSet<Periode>::new))));


        for (Map.Entry<ÅrsakTilVurdering, TreeSet<Periode>> entry : årsakTilVurderingMap.entrySet()) {
            var key = entry.getKey();
            var timeline = result.getOrDefault(key, LocalDateTimeline.empty());

            timeline = timeline.combine(new LocalDateTimeline<>(entry.getValue().stream().map(it -> new LocalDateSegment<>(it.getFom(), it.getTom(), true)).collect(Collectors.toSet())), StandardCombinators::alwaysTrueForMatch, LocalDateTimeline.JoinStyle.CROSS_JOIN);
            result.put(key, timeline.compress());
        }

        return result.entrySet()
            .stream()
            .map(it -> new ÅrsakMedPerioder(it.getKey(), it.getValue()
                .toSegments()
                .stream()
                .map(at -> new Periode(at.getFom(), at.getTom()))
                .collect(Collectors.toSet())))
            .collect(Collectors.toList());
    }

    private Stream<ÅrsakMedPerioder> tilÅrsakMedPerioder(PeriodeMedÅrsaker it) {
        return it.getÅrsaker().stream().map(at -> new ÅrsakMedPerioder(at, new TreeSet<>(Set.of(it.getPeriode()))));
    }

    private Set<ÅrsakTilVurdering> transformerÅrsaker(LocalDateSegment<ÅrsakerTilVurdering> segment) {
        if (segment.getValue() == null) {
            return Set.of();
        }

        var årsaker = segment.getValue().getÅrsaker();

        if (årsaker.size() > 1 && årsaker.contains(ÅrsakTilVurdering.REVURDERER_BERØRT_PERIODE)) {
            return årsaker.stream()
                .filter(it -> !it.equals(ÅrsakTilVurdering.REVURDERER_BERØRT_PERIODE))
                .collect(Collectors.toSet());
        }

        return årsaker;
    }

    private LocalDateTimeline<ÅrsakerTilVurdering> mergeTidslinjer(List<LocalDateTimeline<ÅrsakerTilVurdering>> relevanteTidslinjer, KantIKantVurderer kantIKantVurderer, LocalDateSegmentCombinator<ÅrsakerTilVurdering, ÅrsakerTilVurdering, ÅrsakerTilVurdering> mergeSegments) {
        var tidslinjen = new LocalDateTimeline<ÅrsakerTilVurdering>(List.of());
        for (LocalDateTimeline<ÅrsakerTilVurdering> linje : relevanteTidslinjer) {
            tidslinjen = tidslinjen.combine(linje, mergeSegments, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }
        var segmenterSomMangler = Hjelpetidslinjer.utledHullSomMåTettes(tidslinjen, kantIKantVurderer);
        tidslinjen = tidslinjen.combine(segmenterSomMangler, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        return tidslinjen;
    }

    private ÅrsakTilVurdering utledRevurderingÅrsak(Behandling behandling) {
        if (behandling.erManueltOpprettet() && behandling.erRevurdering()) {
            return ÅrsakTilVurdering.MANUELT_REVURDERER_PERIODE;
        }
        return ÅrsakTilVurdering.REVURDERER_BERØRT_PERIODE;
    }

    private List<KravDokumentMedSøktePerioder> mapKravTilDto(Set<Map.Entry<KravDokument, List<SøktPeriode<VurdertSøktPeriode.SøktPeriodeData>>>> relevanteDokumenterMedPeriode) {
        return relevanteDokumenterMedPeriode.stream()
            .filter(it -> kravDokumentTypeBrukesAvFormidling(it.getKey().getType()))
            .map(it -> new KravDokumentMedSøktePerioder(it.getKey().getJournalpostId(),
                it.getKey().getInnsendingsTidspunkt(),
                KravDokumentType.fraKode(it.getKey().getType().name()),
                it.getValue().stream().map(at -> new no.nav.ung.sak.kontrakt.krav.SøktPeriode(at.getPeriode().tilPeriode(), at.getType(), at.getArbeidsgiver(), at.getArbeidsforholdRef())).collect(Collectors.toList()),
                it.getKey().getKildesystem().getKode()))
            .toList();

    }

    private boolean kravDokumentTypeBrukesAvFormidling(no.nav.ung.sak.perioder.KravDokumentType kravDokumentType) {
        return KravDokumentType.fraKode(kravDokumentType.name()) != null;
    }

    private LocalDateSegment<ÅrsakerTilVurdering> mergeSegmentsAndreDokumenter(LocalDateInterval interval, LocalDateSegment<ÅrsakerTilVurdering> første, LocalDateSegment<ÅrsakerTilVurdering> siste) {
        Set<ÅrsakTilVurdering> årsaker = new HashSet<>();
        Set<no.nav.ung.sak.perioder.KravDokumentType> kravDokumentTyper = new HashSet<>();

        if (første != null && første.getValue() != null && !første.getValue().getÅrsaker().isEmpty()) {
            årsaker.addAll(første.getValue().getÅrsaker());
            kravDokumentTyper.addAll(første.getValue().getKravdokumenterForÅrsaker());
        }
        if (siste != null && siste.getValue() != null && !siste.getValue().getÅrsaker().isEmpty()) {
            årsaker.addAll(siste.getValue().getÅrsaker());
            kravDokumentTyper.addAll(siste.getValue().getKravdokumenterForÅrsaker());
        }

        if (årsaker.contains(ÅrsakTilVurdering.REVURDERER_BERØRT_PERIODE) && årsaker.contains(ÅrsakTilVurdering.FØRSTEGANGSVURDERING)) {
            årsaker = new HashSet<>(utledÅrsakForEndring(kravDokumentTyper));
        }
        if (årsaker.contains(ÅrsakTilVurdering.MANUELT_REVURDERER_PERIODE) && årsaker.contains(ÅrsakTilVurdering.FØRSTEGANGSVURDERING)) {
            årsaker = new HashSet<>(utledÅrsakForEndring(kravDokumentTyper));
            årsaker.add(ÅrsakTilVurdering.MANUELT_REVURDERER_PERIODE);
        }
        if (årsaker.contains(ÅrsakTilVurdering.TRUKKET_KRAV) && årsaker.contains(ÅrsakTilVurdering.FØRSTEGANGSVURDERING)) {
            årsaker = Set.of(ÅrsakTilVurdering.TRUKKET_KRAV);
        }

        return new LocalDateSegment<>(interval, new ÅrsakerTilVurdering(årsaker, kravDokumentTyper));
    }

    private Set<ÅrsakTilVurdering> utledÅrsakForEndring(Set<no.nav.ung.sak.perioder.KravDokumentType> kravDokumentTyper) {
        if (kravDokumentTyper.contains(no.nav.ung.sak.perioder.KravDokumentType.SØKNAD)) {
            return Set.of(ÅrsakTilVurdering.ENDRING_FRA_BRUKER);
        }
        if (kravDokumentTyper.contains(no.nav.ung.sak.perioder.KravDokumentType.INNTEKTSMELDING)
            || kravDokumentTyper.contains(no.nav.ung.sak.perioder.KravDokumentType.INNTEKTSMELDING_UTEN_REFUSJONSKRAV)) {
            return Set.of(ÅrsakTilVurdering.REVURDERER_NY_INNTEKTSMELDING);
        }
        return Collections.emptySet();
    }

    private LocalDateSegment<ÅrsakerTilVurdering> mergeSegments(LocalDateInterval interval, LocalDateSegment<ÅrsakerTilVurdering> første, LocalDateSegment<ÅrsakerTilVurdering> siste) {
        var årsaker = new HashSet<ÅrsakTilVurdering>();
        var kravDokumentTyper = new HashSet<no.nav.ung.sak.perioder.KravDokumentType>();

        if (første != null && første.getValue() != null && !første.getValue().getÅrsaker().isEmpty()) {
            årsaker.addAll(første.getValue().getÅrsaker());
            kravDokumentTyper.addAll(første.getValue().getKravdokumenterForÅrsaker());
        }
        if (siste != null && siste.getValue() != null && !siste.getValue().getÅrsaker().isEmpty()) {
            var sisteÅrsaker = siste.getValue().getÅrsaker();
            if (årsaker.contains(ÅrsakTilVurdering.FØRSTEGANGSVURDERING) && siste.getValue().getÅrsaker().contains(ÅrsakTilVurdering.FØRSTEGANGSVURDERING)) {
                årsaker.add(ÅrsakTilVurdering.ENDRING_FRA_BRUKER);
            }
            årsaker.addAll(sisteÅrsaker);
            kravDokumentTyper.addAll(siste.getValue().getKravdokumenterForÅrsaker());
        }
        return new LocalDateSegment<>(interval, new ÅrsakerTilVurdering(årsaker, kravDokumentTyper));
    }

    private List<LocalDateSegment<ÅrsakerTilVurdering>> tilSegments(Map.Entry<KravDokument, List<SøktPeriode<VurdertSøktPeriode.SøktPeriodeData>>> entry,
                                                                    KantIKantVurderer kantIKantVurderer,
                                                                    ÅrsakTilVurdering årsakTilVurdering) {
        var segmenter = entry.getValue()
            .stream()
            .map(it -> new LocalDateSegment<>(it.getPeriode().toLocalDateInterval(), new ÅrsakerTilVurdering(Set.of(årsakTilVurdering), Set.of(entry.getKey().getType()))))
            .toList();

        var tidslinjen = new LocalDateTimeline<ÅrsakerTilVurdering>(List.of());
        for (LocalDateSegment<ÅrsakerTilVurdering> segment : segmenter) {
            tidslinjen = tidslinjen.combine(segment, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }

        var segmenterSomMangler = Hjelpetidslinjer.utledHullSomMåTettes(tidslinjen, kantIKantVurderer);
        tidslinjen = tidslinjen.combine(segmenterSomMangler, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);

        return tidslinjen.compress()
            .stream()
            .toList();
    }

    private Set<Map.Entry<KravDokument, List<SøktPeriode<VurdertSøktPeriode.SøktPeriodeData>>>> utledKravdokumenterRelevantForPeriodeTilVurdering(
        Set<KravDokument> kravdokumenter,
        Map<KravDokument, List<SøktPeriode<VurdertSøktPeriode.SøktPeriodeData>>> kravdokumenterMedPeriode,
        NavigableSet<DatoIntervallEntitet> perioderTilVurdering) {
        return kravdokumenterMedPeriode.entrySet()
            .stream()
            .filter(it -> kravdokumenter.stream()
                .noneMatch(at -> at.getJournalpostId().equals(it.getKey().getJournalpostId())))
            .filter(it -> perioderTilVurdering.stream().anyMatch(at -> it.getValue().stream().anyMatch(pr -> at.overlapper(pr.getPeriode()))))
            .collect(Collectors.toSet());
    }

    private Set<Map.Entry<KravDokument, List<SøktPeriode<VurdertSøktPeriode.SøktPeriodeData>>>> utledKravdokumenterTilkommetIBehandlingen(Set<KravDokument> kravdokumenter, Map<KravDokument, List<SøktPeriode<VurdertSøktPeriode.SøktPeriodeData>>> kravdokumenterMedPeriode) {
        return kravdokumenterMedPeriode.entrySet()
            .stream()
            .filter(it -> kravdokumenter.stream()
                .anyMatch(at -> at.getJournalpostId().equals(it.getKey().getJournalpostId())))
            .collect(Collectors.toSet());
    }

    private record KravdokumenterPerKravstiller(RolleType rolleType, Arbeidsgiver arbeidsgiver,
                                                KravDokument kravdokumentForBehandling) {
    }
}
