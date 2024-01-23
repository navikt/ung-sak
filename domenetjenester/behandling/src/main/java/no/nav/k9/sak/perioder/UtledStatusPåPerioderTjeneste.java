package no.nav.k9.sak.perioder;

import no.nav.fpsak.tidsserie.*;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.KantIKantVurderer;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.Hjelpetidslinjer;
import no.nav.k9.sak.kontrakt.krav.*;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.Periode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UtledStatusPåPerioderTjeneste {

    private static Logger LOGGER = LoggerFactory.getLogger(UtledStatusPåPerioderTjeneste.class);
    private Boolean filtrereUtTilstøtendePeriode;

    public UtledStatusPåPerioderTjeneste(Boolean filtrereUtTilstøtendePeriode) {
        this.filtrereUtTilstøtendePeriode = filtrereUtTilstøtendePeriode;
    }

    private static Map<KravDokument, List<SøktPeriode<VurdertSøktPeriode.SøktPeriodeData>>> alleKravdokumenterForArbeidsgiver(Map<KravDokument, List<SøktPeriode<VurdertSøktPeriode.SøktPeriodeData>>> alleKravdokumenterMedPeriode, Arbeidsgiver arbeidsgiver) {
        return alleKravdokumenterMedPeriode.entrySet()
            .stream()
            .filter(it -> it.getKey().getType() == no.nav.k9.sak.perioder.KravDokumentType.INNTEKTSMELDING)
            .filter(e -> e.getValue().stream().anyMatch(at -> arbeidsgiver.equals(at.getArbeidsgiver())))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public StatusForPerioderPåBehandling utled(Behandling behandling,
                                               KantIKantVurderer kantIKantVurderer,
                                               Set<KravDokument> kravdokumenter,
                                               Map<KravDokument, List<SøktPeriode<VurdertSøktPeriode.SøktPeriodeData>>> kravdokumenterMedPeriode,
                                               NavigableSet<DatoIntervallEntitet> perioderTilVurdering,
                                               NavigableSet<DatoIntervallEntitet> perioderSomSkalTilbakestilles,
                                               NavigableSet<PeriodeMedÅrsak> revurderingPerioderFraAndreParter) {


        List<PeriodeMedÅrsaker> perioder = perioderMedÅrsaker(behandling,
            kantIKantVurderer,
            kravdokumenter,
            kravdokumenterMedPeriode,
            perioderTilVurdering,
            perioderSomSkalTilbakestilles,
            revurderingPerioderFraAndreParter
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
                revurderingPerioderFraAndreParter,
                perioder);
        } else {
            perioderMedÅrsakPerKravstiller = Set.of(new PerioderMedÅrsakPerKravstiller(RolleType.BRUKER, null, perioder));
        }

        var relevanteDokumenterMedPeriode = utledKravdokumenterTilkommetIBehandlingen(kravdokumenter, kravdokumenterMedPeriode);

        return new StatusForPerioderPåBehandling(perioderTilVurderingSet, perioder, årsakMedPerioder, mapKravTilDto(relevanteDokumenterMedPeriode),
            perioderMedÅrsakPerKravstiller.stream().toList());
    }

    private Set<PerioderMedÅrsakPerKravstiller> perioderMedÅrsakPerKravstiller(
        Behandling behandling,
        KantIKantVurderer kantIKantVurderer,
        Set<KravDokument> kravdokumenter,
        Map<KravDokument, List<SøktPeriode<VurdertSøktPeriode.SøktPeriodeData>>> kravdokumenterMedPeriode,
        NavigableSet<DatoIntervallEntitet> perioderTilVurdering,
        NavigableSet<DatoIntervallEntitet> perioderSomSkalTilbakestilles,
        NavigableSet<PeriodeMedÅrsak> revurderingPerioderFraAndreParter,
        List<PeriodeMedÅrsaker> perioderMedÅrsakPåTversAvKravstillere) {

        Set<PerioderMedÅrsakPerKravstiller> resultat = new HashSet<>();

        if (kravdokumenter.stream().anyMatch(it -> it.getType() == no.nav.k9.sak.perioder.KravDokumentType.SØKNAD)) {
            resultat.add(new PerioderMedÅrsakPerKravstiller(RolleType.BRUKER, null,
                perioderMedÅrsakPåTversAvKravstillere));
        }

        var kravdokumenterPerKravstiller = kravdokumenter
            .stream()
            //Denne er nødvendig fordi type for kravdokumenter som gjelder for behandling ikke mappes til INNTEKTSMELDING_UTEN_REFUSJONSKRAV
            .filter(it -> !erInntektmeldingUtenRefusjonskrav(it, kravdokumenterMedPeriode))
            .filter(it -> it.getType() != no.nav.k9.sak.perioder.KravDokumentType.SØKNAD)
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
                            perioderSomSkalTilbakestilles,
                            revurderingPerioderFraAndreParter
                        ))
                    ));
        }

        return resultat;
    }

    private boolean erInntektmeldingUtenRefusjonskrav(KravDokument kravDokument, Map<KravDokument, List<SøktPeriode<VurdertSøktPeriode.SøktPeriodeData>>> kravdokumenterMedPeriode) {
        return kravdokumenterMedPeriode.keySet().stream()
            .filter(it -> it.getType() == no.nav.k9.sak.perioder.KravDokumentType.INNTEKTSMELDING_UTEN_REFUSJONSKRAV)
            .anyMatch(it -> kravDokument.getJournalpostId().equals(it.getJournalpostId()));
    }

    private KravdokumenterPerKravstiller mapTilKravdokumentPerRolle(
        KravDokument kravdokumentPåBehandling,
        Map<KravDokument, List<SøktPeriode<VurdertSøktPeriode.SøktPeriodeData>>> alleKravdokumenterMedPeriode) {

        RolleType rolleType = utledRolle(kravdokumentPåBehandling.getType());

        if (rolleType == RolleType.ARBEIDSGIVER) {

            Arbeidsgiver arbeidsgiver = alleKravdokumenterMedPeriode.entrySet()
                .stream()
                .filter(it -> it.getKey().getType() != no.nav.k9.sak.perioder.KravDokumentType.INNTEKTSMELDING_UTEN_REFUSJONSKRAV)
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

    private RolleType utledRolle(no.nav.k9.sak.perioder.KravDokumentType type) {
        return type == no.nav.k9.sak.perioder.KravDokumentType.SØKNAD ? RolleType.BRUKER : RolleType.ARBEIDSGIVER;
    }

    private List<PeriodeMedÅrsaker> perioderMedÅrsaker(
        Behandling behandling,
        KantIKantVurderer kantIKantVurderer,
        Set<KravDokument> kravdokumenter,
        Map<KravDokument, List<SøktPeriode<VurdertSøktPeriode.SøktPeriodeData>>> kravdokumenterMedPeriode,
        NavigableSet<DatoIntervallEntitet> perioderTilVurdering,
        NavigableSet<DatoIntervallEntitet> perioderSomSkalTilbakestilles,
        NavigableSet<PeriodeMedÅrsak> revurderingPerioderFraAndreParter) {

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

        var endringFraAndreParter = new LocalDateTimeline<>(revurderingPerioderFraAndreParter.stream()
            .map(entry -> new LocalDateSegment<>(entry.getPeriode().toLocalDateInterval(), new ÅrsakerTilVurdering(Set.of(ÅrsakTilVurdering.mapFra(entry.getÅrsak())))))
            .collect(Collectors.toList()), this::mergeAndreBerørtSaker);
        tidslinje = tidslinje.combine(endringFraAndreParter, this::mergeAndreBerørtSaker, LocalDateTimeline.JoinStyle.CROSS_JOIN);


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
                no.nav.k9.sak.kontrakt.krav.KravDokumentType.fraKode(it.getKey().getType().name()),
                it.getValue().stream().map(at -> new no.nav.k9.sak.kontrakt.krav.SøktPeriode(at.getPeriode().tilPeriode(), at.getType(), at.getArbeidsgiver(), at.getArbeidsforholdRef())).collect(Collectors.toList()),
                it.getKey().getKildesystem().getKode()))
            .toList();

    }

    private boolean kravDokumentTypeBrukesAvFormidling(no.nav.k9.sak.perioder.KravDokumentType kravDokumentType) {
        return no.nav.k9.sak.kontrakt.krav.KravDokumentType.fraKode(kravDokumentType.name()) != null;
    }

    private LocalDateSegment<ÅrsakerTilVurdering> mergeAndreBerørtSaker(LocalDateInterval interval, LocalDateSegment<ÅrsakerTilVurdering> første, LocalDateSegment<ÅrsakerTilVurdering> siste) {
        Set<ÅrsakTilVurdering> årsaker = new HashSet<>();
        Set<no.nav.k9.sak.perioder.KravDokumentType> kravDokumentTyper = new HashSet<>();
        if (første != null && første.getValue() != null) {
            årsaker.addAll(første.getValue().getÅrsaker());
            kravDokumentTyper.addAll(første.getValue().getKravdokumenterForÅrsaker());
        }
        if (siste != null && siste.getValue() != null) {
            årsaker.addAll(siste.getValue().getÅrsaker());
            kravDokumentTyper.addAll(siste.getValue().getKravdokumenterForÅrsaker());
        }
        if (årsaker.contains(ÅrsakTilVurdering.FØRSTEGANGSVURDERING)) {
            if (årsaker.contains(ÅrsakTilVurdering.UTSATT_BEHANDLING)) {
                årsaker = new HashSet<>(Set.of(ÅrsakTilVurdering.FØRSTEGANGSVURDERING, ÅrsakTilVurdering.UTSATT_BEHANDLING));
            } else {
                årsaker = new HashSet<>(Set.of(ÅrsakTilVurdering.FØRSTEGANGSVURDERING));
            }
        }

        return new LocalDateSegment<>(interval, new ÅrsakerTilVurdering(årsaker, kravDokumentTyper));
    }

    private LocalDateSegment<ÅrsakerTilVurdering> mergeSegmentsAndreDokumenter(LocalDateInterval interval, LocalDateSegment<ÅrsakerTilVurdering> første, LocalDateSegment<ÅrsakerTilVurdering> siste) {
        Set<ÅrsakTilVurdering> årsaker = new HashSet<>();
        Set<no.nav.k9.sak.perioder.KravDokumentType> kravDokumentTyper = new HashSet<>();

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

    private Set<ÅrsakTilVurdering> utledÅrsakForEndring(Set<no.nav.k9.sak.perioder.KravDokumentType> kravDokumentTyper) {
        if (kravDokumentTyper.contains(no.nav.k9.sak.perioder.KravDokumentType.SØKNAD)) {
            return Set.of(ÅrsakTilVurdering.ENDRING_FRA_BRUKER);
        }
        if (kravDokumentTyper.contains(no.nav.k9.sak.perioder.KravDokumentType.INNTEKTSMELDING)
            || kravDokumentTyper.contains(no.nav.k9.sak.perioder.KravDokumentType.INNTEKTSMELDING_UTEN_REFUSJONSKRAV)) {
            return Set.of(ÅrsakTilVurdering.REVURDERER_NY_INNTEKTSMELDING);
        }
        return Collections.emptySet();
    }

    private LocalDateSegment<ÅrsakerTilVurdering> mergeSegments(LocalDateInterval interval, LocalDateSegment<ÅrsakerTilVurdering> første, LocalDateSegment<ÅrsakerTilVurdering> siste) {
        var årsaker = new HashSet<ÅrsakTilVurdering>();
        var kravDokumentTyper = new HashSet<no.nav.k9.sak.perioder.KravDokumentType>();

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
