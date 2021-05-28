package no.nav.k9.sak.web.app.tjenester.kravperioder;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.krav.KravDokumentMedSøktePerioder;
import no.nav.k9.sak.kontrakt.krav.KravDokumentType;
import no.nav.k9.sak.kontrakt.krav.PeriodeMedÅrsaker;
import no.nav.k9.sak.kontrakt.krav.StatusForPerioderPåBehandling;
import no.nav.k9.sak.kontrakt.krav.ÅrsakTilVurdering;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.PeriodeMedÅrsak;
import no.nav.k9.sak.perioder.SøktPeriode;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;
import no.nav.k9.sak.typer.Periode;

@Dependent
public class UtledStatusPåPerioderTjeneste {

    public StatusForPerioderPåBehandling utled(Set<KravDokument> kravdokumenter,
                                               Map<KravDokument, List<SøktPeriode<VurdertSøktPeriode.SøktPeriodeData>>> kravdokumenterMedPeriode,
                                               NavigableSet<DatoIntervallEntitet> perioderTilVurdering,
                                               NavigableSet<PeriodeMedÅrsak> revurderingPerioderFraAndreParter) {

        var relevanteDokumenterMedPeriode = utledKravdokumenterTilkommetIBehandlingen(kravdokumenter, kravdokumenterMedPeriode);
        var andreRelevanteDokumenterForPeriodenTilVurdering = utledKravdokumenterRelevantForPeriodeTilVurdering(kravdokumenter, kravdokumenterMedPeriode, perioderTilVurdering);

        var tidslinje = new LocalDateTimeline<ÅrsakerTilVurdering>(List.of());
        var relevanteTidslinjer = relevanteDokumenterMedPeriode.stream()
            .map(entry -> tilSegments(entry, ÅrsakTilVurdering.FØRSTEGANGSVURDERING))
            .map(LocalDateTimeline::new)
            .collect(Collectors.toList());

        for (LocalDateTimeline<ÅrsakerTilVurdering> linje : relevanteTidslinjer) {
            tidslinje = tidslinje.combine(linje, this::mergeSegments, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }

        var endringFraBruker = andreRelevanteDokumenterForPeriodenTilVurdering.stream()
            .map(entry -> tilSegments(entry, ÅrsakTilVurdering.REVURDERER_BERØRT_PERIODE))
            .map(LocalDateTimeline::new)
            .collect(Collectors.toList());

        for (LocalDateTimeline<ÅrsakerTilVurdering> linje : endringFraBruker) {
            tidslinje = tidslinje.combine(linje, this::mergeSegmentsAndreDokumenter, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }
        var endringFraAndreParter = new LocalDateTimeline<>(revurderingPerioderFraAndreParter.stream()
            .map(entry -> new LocalDateSegment<>(entry.getPeriode().toLocalDateInterval(), new ÅrsakerTilVurdering(Set.of(mapFra(entry.getÅrsak())))))
            .collect(Collectors.toList()));

        tidslinje = tidslinje.combine(endringFraAndreParter, this::mergeAndreBerørtSaker, LocalDateTimeline.JoinStyle.CROSS_JOIN)
            .compress();

        var perioder = tidslinje.toSegments()
            .stream()
            .map(it -> new PeriodeMedÅrsaker(new Periode(it.getFom(), it.getTom()), it.getValue() != null ? it.getValue().getÅrsaker() : Set.of()))
            .collect(Collectors.toList());

        var perioderTilVurderingKombinert = new LocalDateTimeline<>(perioderTilVurdering.stream().map(it -> new LocalDateSegment<>(it.getFomDato(), it.getTomDato(), true)).collect(Collectors.toList()))
            .compress()
            .toSegments()
            .stream()
            .map(it -> DatoIntervallEntitet.fraOgMedTilOgMed(it.getFom(), it.getTom()))
            .collect(Collectors.toCollection(TreeSet::new));

        return new StatusForPerioderPåBehandling(perioderTilVurderingKombinert.stream().map(DatoIntervallEntitet::tilPeriode).collect(Collectors.toSet()), perioder, mapKravTilDto(relevanteDokumenterMedPeriode));
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

    private List<LocalDateSegment<ÅrsakerTilVurdering>> tilSegments(Map.Entry<KravDokument, List<SøktPeriode<VurdertSøktPeriode.SøktPeriodeData>>> entry, ÅrsakTilVurdering årsakTilVurdering) {
        return entry.getValue().stream().map(it -> new LocalDateSegment<>(it.getPeriode().toLocalDateInterval(), new ÅrsakerTilVurdering(Set.of(årsakTilVurdering)))).collect(Collectors.toList());
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

    private ÅrsakTilVurdering mapFra(BehandlingÅrsakType type) {
        if (type == BehandlingÅrsakType.G_REGULERING) {
            return ÅrsakTilVurdering.G_REGULERING;
        }
        if (type == BehandlingÅrsakType.RE_ENDRING_FRA_ANNEN_OMSORGSPERSON) {
            return ÅrsakTilVurdering.REVURDERER_ENDRING_FRA_ANNEN_PART;
        }
        throw new IllegalArgumentException("Ukjent type " + type);
    }
}
