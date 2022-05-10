package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn;

import java.util.List;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.LocalDateTimeline.JoinStyle;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomUtils;

@Dependent
public class EndringUnntakEtablertTilsynTjeneste {

    private UnntakEtablertTilsynGrunnlagRepository unntakEtablertTilsynGrunnlagRepository;

    @Inject
    public EndringUnntakEtablertTilsynTjeneste(UnntakEtablertTilsynGrunnlagRepository unntakEtablertTilsynGrunnlagRepository) {
        this.unntakEtablertTilsynGrunnlagRepository = unntakEtablertTilsynGrunnlagRepository;
    }


    public NavigableSet<DatoIntervallEntitet> utledRelevanteEndringerSidenBehandling(Long behandlingId, AktørId pleietrengende) {
        final Optional<UnntakEtablertTilsynGrunnlag> eksisterendeGrunnlag = unntakEtablertTilsynGrunnlagRepository.hentHvisEksisterer(behandlingId);
        final Optional<UnntakEtablertTilsynForPleietrengende> unntakEtablertTilsynForPleietrengende = unntakEtablertTilsynGrunnlagRepository.hentHvisEksistererUnntakPleietrengende(pleietrengende);
        return utledEndringer(eksisterendeGrunnlag.map(UnntakEtablertTilsynGrunnlag::getUnntakEtablertTilsynForPleietrengende), unntakEtablertTilsynForPleietrengende);
    }

    public boolean harEndringerSidenBehandling(Long behandlingId, AktørId pleietrengende) {
        var endringer = utledRelevanteEndringerSidenBehandling(behandlingId, pleietrengende);

        return !endringer.isEmpty();
    }

    public LocalDateTimeline<Boolean> perioderMedEndringerSidenBehandling(Long behandlingId, AktørId pleietrengende) {
        return SykdomUtils.toLocalDateTimeline(utledRelevanteEndringerSidenBehandling(behandlingId, pleietrengende));
    }

    NavigableSet<DatoIntervallEntitet> utledEndringer(Optional<UnntakEtablertTilsynForPleietrengende> eksisterendeGrunnlag,
                                                      Optional<UnntakEtablertTilsynForPleietrengende> nyttGrunnlag) {

        final LocalDateTimeline<Boolean> nattevåkendringer = utledEndringerMedTidslinje(toNattevåkTidslinje(eksisterendeGrunnlag), toNattevåkTidslinje(nyttGrunnlag));
        final LocalDateTimeline<Boolean> beredskapendringer = utledEndringerMedTidslinje(toBeredskapTidslinje(eksisterendeGrunnlag), toBeredskapTidslinje(nyttGrunnlag));

        return DatoIntervallEntitet.fraTimeline(nattevåkendringer.union(beredskapendringer, StandardCombinators::coalesceLeftHandSide));
    }

    private LocalDateTimeline<Boolean> utledEndringerMedTidslinje(LocalDateTimeline<UnntakEtablertTilsynPeriode> eksisterende,
                                                                  LocalDateTimeline<UnntakEtablertTilsynPeriode> ny) {
        return eksisterende.combine(ny, (datoInterval, datoSegment, datoSegment2) -> {
            if (datoSegment == null
                || datoSegment2 == null
                || datoSegment.getValue().getResultat() != datoSegment2.getValue().getResultat()) {
                return new LocalDateSegment<>(datoInterval, Boolean.TRUE);
            }
            return null;
        }, JoinStyle.CROSS_JOIN).compress();
    }

    private LocalDateTimeline<UnntakEtablertTilsynPeriode> toNattevåkTidslinje(Optional<UnntakEtablertTilsynForPleietrengende> grunnlag) {
        return toTidslinje(grunnlag.map(u -> u.getNattevåk().getPerioder()).orElse(List.of()));
    }

    private LocalDateTimeline<UnntakEtablertTilsynPeriode> toBeredskapTidslinje(Optional<UnntakEtablertTilsynForPleietrengende> grunnlag) {
        return toTidslinje(grunnlag.map(u -> u.getBeredskap().getPerioder()).orElse(List.of()));
    }

    private LocalDateTimeline<UnntakEtablertTilsynPeriode> toTidslinje(List<UnntakEtablertTilsynPeriode> perioder) {
        final var segments = perioder
            .stream()
            .map(p -> new LocalDateSegment<>(p.getPeriode().getFomDato(), p.getPeriode().getTomDato(), p))
            .collect(Collectors.toList());
        final LocalDateTimeline<UnntakEtablertTilsynPeriode> tidslinje = new LocalDateTimeline<>(segments);
        return tidslinje;
    }
}
