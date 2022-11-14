package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.institusjon;

import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.institusjon.InstitusjonGodkjenningStatus.GODKJENT;
import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.institusjon.InstitusjonGodkjenningStatus.IKKE_GODKJENT;
import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.institusjon.InstitusjonGodkjenningStatus.MANGLER_VURDERING;

import java.time.LocalDate;
import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.GodkjentOpplæringsinstitusjonTjeneste;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertInstitusjon;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;

public class VurderInstitusjonTjeneste {

    private final GodkjentOpplæringsinstitusjonTjeneste godkjentOpplæringsinstitusjonTjeneste;

    public VurderInstitusjonTjeneste(GodkjentOpplæringsinstitusjonTjeneste godkjentOpplæringsinstitusjonTjeneste) {
        this.godkjentOpplæringsinstitusjonTjeneste = godkjentOpplæringsinstitusjonTjeneste;
    }

    public LocalDateTimeline<InstitusjonGodkjenningStatus> hentTidslinjeTilVurderingMedInstitusjonsGodkjenning(Set<PerioderFraSøknad> perioderFraSøknad, VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag, NavigableSet<DatoIntervallEntitet> perioderTilVurdering) {

        var tidslinjeTilVurdering = TidslinjeUtil.tilTidslinjeKomprimert(perioderTilVurdering)
            .map(segment -> List.of(new LocalDateSegment<>(segment.getLocalDateInterval(), true)));

        LocalDateTimeline<InstitusjonGodkjenningStatus> tidslinjeMedInstitusjonsgodkjenning = lagTidslinjeMedInstitusjonsGodkjenning(perioderFraSøknad, vurdertOpplæringGrunnlag);

        return tidslinjeMedInstitusjonsgodkjenning.intersection(tidslinjeTilVurdering);
    }

    private LocalDateTimeline<InstitusjonGodkjenningStatus> lagTidslinjeMedInstitusjonsGodkjenning(Set<PerioderFraSøknad> perioderFraSøknad, VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag) {

        LocalDateTimeline<List<InstitusjonFraSøknad>> tidslinjeMedInstitusjonsnavn = hentTidslinjeMedInstitusjonFraSøknad(perioderFraSøknad);

        LocalDateTimeline<InstitusjonGodkjenningStatus> tidslinjeMedStatus = tidslinjeMedInstitusjonsnavn.mapValue(v -> MANGLER_VURDERING);
        for (LocalDateSegment<List<InstitusjonFraSøknad>> segment : tidslinjeMedInstitusjonsnavn) {
            var vurderTidslinje = mapInstitusjonsVurderingForSegment(vurdertOpplæringGrunnlag, segment)
                .filterValue(v -> Set.of(GODKJENT, IKKE_GODKJENT).contains(v))
                .compress();
            tidslinjeMedStatus = tidslinjeMedStatus.combine(vurderTidslinje, this::mergeVurderinger, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }
        return tidslinjeMedStatus.compress();
    }

    private LocalDateTimeline<InstitusjonGodkjenningStatus> mapInstitusjonsVurderingForSegment(VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag, LocalDateSegment<List<InstitusjonFraSøknad>> segment) {
        Objects.requireNonNull(segment);
        if (segment.getValue().isEmpty()) {
            throw new IllegalStateException("Forventet minst en søknad med institusjonsopphold");
        }

        var opplæringGrunnlag = Optional.ofNullable(vurdertOpplæringGrunnlag);
        var manglerVurdering = new LocalDateTimeline<>(List.of(segment));
        var tidslinje = new LocalDateTimeline<>(List.of(segment));

        for (InstitusjonFraSøknad institusjonFraSøknad : segment.getValue()) {
            var godkjentOpplæringsinstitusjon = godkjentOpplæringsinstitusjonTjeneste.hentMedUuid(institusjonFraSøknad.getUuid());
            if (godkjentOpplæringsinstitusjon.isPresent()) {
                LocalDateTimeline<InstitusjonGodkjenningStatus> godkjentTidslinje = godkjentOpplæringsinstitusjon.get().getTidslinje().intersection(tidslinje).mapValue(v -> GODKJENT);
                manglerVurdering = manglerVurdering.disjoint(godkjentTidslinje);
            }
            if (opplæringGrunnlag.isPresent() && !manglerVurdering.isEmpty()) {
                Optional<VurdertInstitusjon> vurdertInstitusjon = vurdertOpplæringGrunnlag.getVurdertInstitusjonHolder().finnVurderingForJournalpostId(institusjonFraSøknad.getJournalpostId());

                if (vurdertInstitusjon.isPresent()) {
                    var vurdering = vurdertInstitusjon.get().getGodkjent() ? GODKJENT : IKKE_GODKJENT;
                    return tidslinje.mapValue(v -> GODKJENT)
                        .combine(manglerVurdering.mapValue(v -> vurdering), StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
                }
            }

            if (manglerVurdering.isEmpty()) {
                return tidslinje.mapValue(v -> GODKJENT);
            }
        }
        return tidslinje.mapValue(v -> GODKJENT)
            .combine(manglerVurdering.mapValue(v -> MANGLER_VURDERING), StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
    }

    private LocalDateSegment<InstitusjonGodkjenningStatus> mergeVurderinger(LocalDateInterval interval, LocalDateSegment<InstitusjonGodkjenningStatus> leftside, LocalDateSegment<InstitusjonGodkjenningStatus> rightSide) {
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

    private LocalDateTimeline<List<InstitusjonFraSøknad>> hentTidslinjeMedInstitusjonFraSøknad(Set<PerioderFraSøknad> perioderFraSøknad) {
        Objects.requireNonNull(perioderFraSøknad);

        NavigableSet<LocalDateSegment<InstitusjonFraSøknad>> segments = new TreeSet<>();
        for (PerioderFraSøknad fraSøknad : perioderFraSøknad) {
            segments.addAll(fraSøknad.getKurs().stream()
                .map(kursPeriode -> {
                    LocalDate fomDato = kursPeriode.getReiseperiodeTil() != null ? kursPeriode.getReiseperiodeTil().getFomDato() : kursPeriode.getPeriode().getFomDato();
                    LocalDate tomDato = kursPeriode.getReiseperiodeHjem() != null ? kursPeriode.getReiseperiodeHjem().getTomDato() :  kursPeriode.getPeriode().getTomDato();
                    return new LocalDateSegment<>(fomDato, tomDato, new InstitusjonFraSøknad(fraSøknad.getJournalpostId(), kursPeriode.getInstitusjon(), kursPeriode.getInstitusjonUuid()));
                })
                .collect(Collectors.toCollection(TreeSet::new)));
        }

        return LocalDateTimeline.buildGroupOverlappingSegments(segments);
    }

    private static class InstitusjonFraSøknad {
        private final JournalpostId journalpostId;
        private final String navn;
        private final UUID uuid;

        InstitusjonFraSøknad(JournalpostId journalpostId, String navn, UUID uuid) {
            this.journalpostId = journalpostId;
            this.navn = navn;
            this.uuid = uuid;
        }

        String getNavn() {
            return navn;
        }

        UUID getUuid() {
            return uuid;
        }

        public JournalpostId getJournalpostId() {
            return journalpostId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            InstitusjonFraSøknad that = (InstitusjonFraSøknad) o;
            return Objects.equals(journalpostId, that.journalpostId)
                && Objects.equals(navn, that.navn)
                && Objects.equals(uuid, that.uuid);
        }

        @Override
        public int hashCode() {
            return Objects.hash(journalpostId, navn, uuid);
        }
    }
}
