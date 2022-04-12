package no.nav.k9.sak.web.app.tjenester.kravperioder;

import java.util.ArrayList;
import java.util.Collection;
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
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.KantIKantVurderer;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.krav.KravDokumentMedSøktePerioder;
import no.nav.k9.sak.kontrakt.krav.KravDokumentType;
import no.nav.k9.sak.kontrakt.krav.PeriodeMedÅrsaker;
import no.nav.k9.sak.kontrakt.krav.StatusForPerioderPåBehandling;
import no.nav.k9.sak.kontrakt.krav.ÅrsakMedPerioder;
import no.nav.k9.sak.kontrakt.krav.ÅrsakTilVurdering;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.PeriodeMedÅrsak;
import no.nav.k9.sak.perioder.SøktPeriode;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;
import no.nav.k9.sak.typer.Periode;

public class UtledStatusPåPerioderTjeneste {

    public UtledStatusPåPerioderTjeneste() {
    }

    public StatusForPerioderPåBehandling utled(Behandling behandling,
                                               KantIKantVurderer kantIKantVurderer,
                                               Set<KravDokument> kravdokumenter,
                                               Map<KravDokument, List<SøktPeriode<VurdertSøktPeriode.SøktPeriodeData>>> kravdokumenterMedPeriode,
                                               NavigableSet<DatoIntervallEntitet> perioderTilVurdering,
                                               NavigableSet<DatoIntervallEntitet> perioderSomSkalTilbakestilles,
                                               NavigableSet<PeriodeMedÅrsak> revurderingPerioderFraAndreParter) {

        var relevanteDokumenterMedPeriode = utledKravdokumenterTilkommetIBehandlingen(kravdokumenter, kravdokumenterMedPeriode);
        var andreRelevanteDokumenterForPeriodenTilVurdering = utledKravdokumenterRelevantForPeriodeTilVurdering(kravdokumenter, kravdokumenterMedPeriode, perioderTilVurdering);
        var perioderTilVurderingKombinert = new LocalDateTimeline<>(perioderTilVurdering.stream().map(it -> new LocalDateSegment<>(it.getFomDato(), it.getTomDato(), true)).collect(Collectors.toList()), StandardCombinators::alwaysTrueForMatch)
            .compress();

        var tidslinje = new LocalDateTimeline<ÅrsakerTilVurdering>(List.of());
        var relevanteTidslinjer = relevanteDokumenterMedPeriode.stream()
            .map(entry -> tilSegments(entry, kantIKantVurderer, ÅrsakTilVurdering.FØRSTEGANGSVURDERING))
            .map(LocalDateTimeline::new)
            .toList();

        tidslinje = mergeTidslinjer(relevanteTidslinjer, kantIKantVurderer, this::mergeSegments);

        var tilbakestillingSegmenter = perioderSomSkalTilbakestilles.stream()
            .map(it -> new LocalDateSegment<>(it.getFomDato(), it.getTomDato(), new ÅrsakerTilVurdering(Set.of(ÅrsakTilVurdering.TRUKKET_KRAV))))
            .collect(Collectors.toList());

        tidslinje = tidslinje.combine(new LocalDateTimeline<>(tilbakestillingSegmenter, StandardCombinators::coalesceRightHandSide), this::mergeSegmentsAndreDokumenter, LocalDateTimeline.JoinStyle.CROSS_JOIN);

        var endringFraBruker = andreRelevanteDokumenterForPeriodenTilVurdering.stream()
            .map(entry -> tilSegments(entry, kantIKantVurderer, utledRevurderingÅrsak(behandling)))
            .map(LocalDateTimeline::new)
            .toList();

        var endringFraBrukerTidslinje = mergeTidslinjer(endringFraBruker, kantIKantVurderer, this::mergeSegmentsAndreDokumenter);
        tidslinje = tidslinje.combine(endringFraBrukerTidslinje, this::mergeSegmentsAndreDokumenter, LocalDateTimeline.JoinStyle.CROSS_JOIN);

        for (PeriodeMedÅrsak entry : revurderingPerioderFraAndreParter) {
            var endringFraAndreParter = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(entry.getPeriode().toLocalDateInterval(), new ÅrsakerTilVurdering(Set.of(ÅrsakTilVurdering.mapFra(entry.getÅrsak()))))));
            tidslinje = tidslinje.combine(endringFraAndreParter, this::mergeAndreBerørtSaker, LocalDateTimeline.JoinStyle.CROSS_JOIN).compress();
        }
        tidslinje = tidslinje.intersection(perioderTilVurderingKombinert);

        var perioder = tidslinje.compress()
            .toSegments()
            .stream()
            .map(it -> new PeriodeMedÅrsaker(new Periode(it.getFom(), it.getTom()), transformerÅrsaker(it)))
            .collect(Collectors.toList());

        var årsakMedPerioder = utledÅrsakMedPerioder(perioder);

        var perioderTilVurderingSet = perioderTilVurderingKombinert.toSegments()
            .stream()
            .map(it -> DatoIntervallEntitet.fraOgMedTilOgMed(it.getFom(), it.getTom()))
            .collect(Collectors.toCollection(TreeSet::new))
            .stream()
            .map(DatoIntervallEntitet::tilPeriode)
            .collect(Collectors.toSet());

        return new StatusForPerioderPåBehandling(perioderTilVurderingSet, perioder, årsakMedPerioder, mapKravTilDto(relevanteDokumenterMedPeriode));
    }

    private List<ÅrsakMedPerioder> utledÅrsakMedPerioder(List<PeriodeMedÅrsaker> perioder) {
        var result = new HashMap<ÅrsakTilVurdering, LocalDateTimeline<Boolean>>();
        var årsakTilVurderingMap = perioder.stream().flatMap(this::tilÅrsakMedPerioder).collect(Collectors.groupingBy(ÅrsakMedPerioder::getÅrsak,
            Collectors.flatMapping(it -> Stream.of(it.getPerioder()).flatMap(Collection::stream), Collectors.toCollection(TreeSet::new))));


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
        var segmenterSomMangler = utledHullSomMåTettes(tidslinjen, kantIKantVurderer);
        for (LocalDateSegment<ÅrsakerTilVurdering> segment : segmenterSomMangler) {
            tidslinjen = tidslinjen.combine(segment, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }
        return tidslinjen;
    }

    private ÅrsakTilVurdering utledRevurderingÅrsak(Behandling behandling) {
        if (behandling.erManueltOpprettet() && behandling.erRevurdering()) {
            return ÅrsakTilVurdering.MANUELT_REVURDERER_PERIODE;
        }
        return ÅrsakTilVurdering.REVURDERER_BERØRT_PERIODE;
    }

    private List<KravDokumentMedSøktePerioder> mapKravTilDto(Set<Map.Entry<KravDokument, List<SøktPeriode<VurdertSøktPeriode.SøktPeriodeData>>>> relevanteDokumenterMedPeriode) {
        return relevanteDokumenterMedPeriode.stream().map(it -> new KravDokumentMedSøktePerioder(it.getKey().getJournalpostId(),
                it.getKey().getInnsendingsTidspunkt(),
                KravDokumentType.fraKode(it.getKey().getType().name()),
                it.getValue().stream().map(at -> new no.nav.k9.sak.kontrakt.krav.SøktPeriode(at.getPeriode().tilPeriode(), at.getType(), at.getArbeidsgiver(), at.getArbeidsforholdRef())).collect(Collectors.toList())))
            .collect(Collectors.toList());

    }

    private LocalDateSegment<ÅrsakerTilVurdering> mergeAndreBerørtSaker(LocalDateInterval interval, LocalDateSegment<ÅrsakerTilVurdering> første, LocalDateSegment<ÅrsakerTilVurdering> siste) {
        Set<ÅrsakTilVurdering> årsaker = new HashSet<>();
        if (første != null && første.getValue() != null && !første.getValue().getÅrsaker().isEmpty()) {
            årsaker.addAll(første.getValue().getÅrsaker());
        }
        if (siste != null && siste.getValue() != null && !siste.getValue().getÅrsaker().isEmpty()) {
            årsaker.addAll(siste.getValue().getÅrsaker());
        }
        if (årsaker.contains(ÅrsakTilVurdering.FØRSTEGANGSVURDERING)) {
            årsaker = new HashSet<>(Set.of(ÅrsakTilVurdering.FØRSTEGANGSVURDERING));
        }

        return new LocalDateSegment<>(interval, new ÅrsakerTilVurdering(årsaker));
    }

    private LocalDateSegment<ÅrsakerTilVurdering> mergeSegmentsAndreDokumenter(LocalDateInterval interval, LocalDateSegment<ÅrsakerTilVurdering> første, LocalDateSegment<ÅrsakerTilVurdering> siste) {
        Set<ÅrsakTilVurdering> årsaker = new HashSet<>();
        if (første != null && første.getValue() != null && !første.getValue().getÅrsaker().isEmpty()) {
            årsaker.addAll(første.getValue().getÅrsaker());
        }
        if (siste != null && siste.getValue() != null && !siste.getValue().getÅrsaker().isEmpty()) {
            årsaker.addAll(siste.getValue().getÅrsaker());
        }

        if (årsaker.contains(ÅrsakTilVurdering.REVURDERER_BERØRT_PERIODE) && årsaker.contains(ÅrsakTilVurdering.FØRSTEGANGSVURDERING)) {
            årsaker = Set.of(ÅrsakTilVurdering.ENDRING_FRA_BRUKER);
        }
        if (årsaker.contains(ÅrsakTilVurdering.MANUELT_REVURDERER_PERIODE) && årsaker.contains(ÅrsakTilVurdering.FØRSTEGANGSVURDERING)) {
            årsaker = Set.of(ÅrsakTilVurdering.MANUELT_REVURDERER_PERIODE, ÅrsakTilVurdering.ENDRING_FRA_BRUKER);
        }
        if (årsaker.contains(ÅrsakTilVurdering.TRUKKET_KRAV) && årsaker.contains(ÅrsakTilVurdering.FØRSTEGANGSVURDERING)) {
            årsaker = Set.of(ÅrsakTilVurdering.TRUKKET_KRAV);
        }

        return new LocalDateSegment<>(interval, new ÅrsakerTilVurdering(årsaker));
    }

    private LocalDateSegment<ÅrsakerTilVurdering> mergeSegments(LocalDateInterval interval, LocalDateSegment<ÅrsakerTilVurdering> første, LocalDateSegment<ÅrsakerTilVurdering> siste) {
        var årsaker = new HashSet<ÅrsakTilVurdering>();
        if (første != null && første.getValue() != null && !første.getValue().getÅrsaker().isEmpty()) {
            årsaker.addAll(første.getValue().getÅrsaker());
        }
        if (siste != null && siste.getValue() != null && !siste.getValue().getÅrsaker().isEmpty()) {
            var sisteÅrsaker = siste.getValue().getÅrsaker();
            if (årsaker.contains(ÅrsakTilVurdering.FØRSTEGANGSVURDERING) && siste.getValue().getÅrsaker().contains(ÅrsakTilVurdering.FØRSTEGANGSVURDERING)) {
                årsaker.add(ÅrsakTilVurdering.ENDRING_FRA_BRUKER);
            }
            årsaker.addAll(sisteÅrsaker);
        }
        return new LocalDateSegment<>(interval, new ÅrsakerTilVurdering(årsaker));
    }

    private List<LocalDateSegment<ÅrsakerTilVurdering>> tilSegments(Map.Entry<KravDokument, List<SøktPeriode<VurdertSøktPeriode.SøktPeriodeData>>> entry,
                                                                    KantIKantVurderer kantIKantVurderer,
                                                                    ÅrsakTilVurdering årsakTilVurdering) {
        var segmenter = entry.getValue()
            .stream()
            .map(it -> new LocalDateSegment<>(it.getPeriode().toLocalDateInterval(), new ÅrsakerTilVurdering(Set.of(årsakTilVurdering))))
            .toList();

        var tidslinjen = new LocalDateTimeline<ÅrsakerTilVurdering>(List.of());
        for (LocalDateSegment<ÅrsakerTilVurdering> segment : segmenter) {
            tidslinjen = tidslinjen.combine(segment, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }

        var segmenterSomMangler = utledHullSomMåTettes(tidslinjen, kantIKantVurderer);
        for (LocalDateSegment<ÅrsakerTilVurdering> segment : segmenterSomMangler) {
            tidslinjen = tidslinjen.combine(segment, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }

        return tidslinjen.compress()
            .toSegments()
            .stream()
            .toList();
    }

    private List<LocalDateSegment<ÅrsakerTilVurdering>> utledHullSomMåTettes(LocalDateTimeline<ÅrsakerTilVurdering> tidslinjen, KantIKantVurderer kantIKantVurderer) {
        var segmenter = tidslinjen.compress().toSegments();

        LocalDateSegment<ÅrsakerTilVurdering> periode = null;
        var resultat = new ArrayList<LocalDateSegment<ÅrsakerTilVurdering>>();

        for (LocalDateSegment<ÅrsakerTilVurdering> segment : segmenter) {
            if (periode == null) {
                periode = segment;
            } else if (kantIKantVurderer.erKantIKant(DatoIntervallEntitet.fra(segment.getLocalDateInterval()), DatoIntervallEntitet.fra(periode.getLocalDateInterval()))) {
                resultat.add(new LocalDateSegment<>(periode.getFom(), segment.getTom(), periode.getValue()));
            } else {
                periode = segment;
            }
        }

        return resultat;
    }

    private Set<Map.Entry<KravDokument, List<SøktPeriode<VurdertSøktPeriode.SøktPeriodeData>>>> utledKravdokumenterRelevantForPeriodeTilVurdering(Set<KravDokument> kravdokumenter,
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
}
