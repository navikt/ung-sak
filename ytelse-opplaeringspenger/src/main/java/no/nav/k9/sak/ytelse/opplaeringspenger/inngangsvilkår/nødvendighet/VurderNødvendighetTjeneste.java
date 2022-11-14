package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet;

import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.NødvendighetGodkjenningStatus.GODKJENT;
import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.NødvendighetGodkjenningStatus.IKKE_GODKJENT;
import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.NødvendighetGodkjenningStatus.MANGLER_VURDERING;

import java.time.LocalDate;
import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæring;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;

class VurderNødvendighetTjeneste {

    public LocalDateTimeline<NødvendighetGodkjenningStatus> hentTidslinjeTilVurderingMedNødvendighetsGodkjenning(Set<PerioderFraSøknad> perioderFraSøknad, VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag, LocalDateTimeline<Boolean> tidslinjeTilVurdering) {
        Objects.requireNonNull(perioderFraSøknad);
        Objects.requireNonNull(tidslinjeTilVurdering);

        LocalDateTimeline<NødvendighetGodkjenningStatus> tidslinjeMedNødvendighetsgodkjenning = lagTidslinjeMedNødvendighetsGodkjenning(perioderFraSøknad, vurdertOpplæringGrunnlag);
        return tidslinjeMedNødvendighetsgodkjenning.intersection(tidslinjeTilVurdering);
    }

    private LocalDateTimeline<NødvendighetGodkjenningStatus> lagTidslinjeMedNødvendighetsGodkjenning(Set<PerioderFraSøknad> perioderFraSøknad, VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag) {

        LocalDateTimeline<List<JournalpostId>> tidslinjeMedJournalpostId = hentTidslinjeMedJournalpostIdFraSøknad(perioderFraSøknad);

        LocalDateTimeline<NødvendighetGodkjenningStatus> tidslinjeMedStatus = tidslinjeMedJournalpostId.mapValue(v -> MANGLER_VURDERING);
        for (LocalDateSegment<List<JournalpostId>> segment : tidslinjeMedJournalpostId) {
            var vurderTidslinje = mapNødvendighetsVurderingForSegment(vurdertOpplæringGrunnlag, segment)
                .filterValue(v -> Set.of(GODKJENT, IKKE_GODKJENT).contains(v))
                .compress();
            tidslinjeMedStatus = tidslinjeMedStatus.combine(vurderTidslinje, this::mergeVurderinger, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }
        return tidslinjeMedStatus.compress();
    }

    private LocalDateTimeline<NødvendighetGodkjenningStatus> mapNødvendighetsVurderingForSegment(VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag, LocalDateSegment<List<JournalpostId>> segment) {
        Objects.requireNonNull(segment);
        if (segment.getValue().isEmpty()) {
            throw new IllegalStateException("Forventet minst en søknad med institusjonsopphold");
        }

        var opplæringGrunnlag = Optional.ofNullable(vurdertOpplæringGrunnlag);
        var tidslinje = new LocalDateTimeline<>(List.of(segment));

        if (opplæringGrunnlag.isPresent() && vurdertOpplæringGrunnlag.getVurdertOpplæringHolder() != null) {
            for (JournalpostId journalpostId : segment.getValue()) {
                Optional<VurdertOpplæring> vurdertOpplæring = vurdertOpplæringGrunnlag.getVurdertOpplæringHolder().finnVurderingForJournalpostId(journalpostId.getJournalpostId());

                if (vurdertOpplæring.isPresent()) {
                    return tidslinje.mapValue(v -> vurdertOpplæring.get().getNødvendigOpplæring() ? GODKJENT : IKKE_GODKJENT);
                }
            }
        }
        return tidslinje.mapValue(v -> MANGLER_VURDERING);
    }

    private LocalDateSegment<NødvendighetGodkjenningStatus> mergeVurderinger(LocalDateInterval interval, LocalDateSegment<NødvendighetGodkjenningStatus> leftside, LocalDateSegment<NødvendighetGodkjenningStatus> rightSide) {
        if (leftside.getValue() != null && Objects.equals(leftside.getValue(), GODKJENT)) {
            return new LocalDateSegment<>(interval, GODKJENT);
        }
        if (rightSide != null && rightSide.getValue() != null && Objects.equals(rightSide.getValue(), GODKJENT)) {
            return new LocalDateSegment<>(interval, GODKJENT);
        }
        if (leftside.getValue() != null && Objects.equals(leftside.getValue(), IKKE_GODKJENT)) {
            return new LocalDateSegment<>(interval, IKKE_GODKJENT);
        }
        if (rightSide != null && rightSide.getValue() != null && Objects.equals(rightSide.getValue(), IKKE_GODKJENT)) {
            return new LocalDateSegment<>(interval, IKKE_GODKJENT);
        }
        return new LocalDateSegment<>(interval, MANGLER_VURDERING);
    }

    private LocalDateTimeline<List<JournalpostId>> hentTidslinjeMedJournalpostIdFraSøknad(Set<PerioderFraSøknad> perioderFraSøknad) {
        NavigableSet<LocalDateSegment<JournalpostId>> segments = new TreeSet<>();
        for (PerioderFraSøknad fraSøknad : perioderFraSøknad) {
            segments.addAll(fraSøknad.getKurs().stream()
                .map(kursPeriode -> {
                    LocalDate fomDato = kursPeriode.getReiseperiodeTil() != null ? kursPeriode.getReiseperiodeTil().getFomDato() : kursPeriode.getPeriode().getFomDato();
                    LocalDate tomDato = kursPeriode.getReiseperiodeHjem() != null ? kursPeriode.getReiseperiodeHjem().getTomDato() :  kursPeriode.getPeriode().getTomDato();
                    return new LocalDateSegment<>(fomDato, tomDato, fraSøknad.getJournalpostId());
                })
                .collect(Collectors.toCollection(TreeSet::new)));
        }

        return LocalDateTimeline.buildGroupOverlappingSegments(segments);
    }
}
