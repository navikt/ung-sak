package no.nav.k9.sak.ytelse.ung.periode;

import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.KantIKantVurderer;
import no.nav.k9.sak.domene.typer.tid.AbstractLocalDateInterval;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@Dependent
public class UngdomsprogramPeriodeTjeneste {

    private final UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;

    @Inject
    public UngdomsprogramPeriodeTjeneste(UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository) {
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
    }


    public LocalDateTimeline<Boolean> finnPeriodeTidslinje(Long behandlingId, KantIKantVurderer kantIKantVurderer) {
        var ungdomsprogramPeriodeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandlingId);
        var periodeTidslinje = lagPeriodeTidslinje(ungdomsprogramPeriodeGrunnlag, kantIKantVurderer);
        return periodeTidslinje;
    }

    public LocalDateTimeline<Boolean> finnEndretPeriodeTidslinje(Long behandlingId, KantIKantVurderer kantIKantVurderer) {
        var initieltGrunnlag = ungdomsprogramPeriodeRepository.hentInitieltGrunnlag(behandlingId);
        var ungdomsprogramPeriodeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandlingId);
        var periodeTidslinje = lagPeriodeTidslinje(ungdomsprogramPeriodeGrunnlag, kantIKantVurderer);
        var initiellPeriodeTidslinje = lagPeriodeTidslinje(initieltGrunnlag, kantIKantVurderer);
        var endretPerioderTidslinje = initiellPeriodeTidslinje.crossJoin(periodeTidslinje, UngdomsprogramPeriodeTjeneste::erEndret)
            .filterValue(v -> v);
        return endretPerioderTidslinje;
    }

    private LocalDateTimeline<Boolean> lagPeriodeTidslinje(Optional<UngdomsprogramPeriodeGrunnlag> ungdomsprogramPeriodeGrunnlag, KantIKantVurderer kantIKantVurderer) {
        return ungdomsprogramPeriodeGrunnlag.stream()
            .flatMap(gr -> gr.getUngdomsprogramPerioder().getPerioder().stream())
            .map(this::bestemPeriode)
            .map(p -> new LocalDateTimeline<>(p.getFomDato(), p.getTomDato(), true))
            .reduce(LocalDateTimeline::crossJoin)
            .map(t -> komprimer(t, kantIKantVurderer))
            .orElse(LocalDateTimeline.empty());
    }

    private static LocalDateSegment<Boolean> erEndret(LocalDateInterval di, LocalDateSegment<Boolean> lhs, LocalDateSegment<Boolean> rhs) {
        return new LocalDateSegment<>(di, lhs == null || rhs == null || lhs.getValue().equals(rhs.getValue()));
    }


    private LocalDateTimeline<Boolean> komprimer(LocalDateTimeline<Boolean> t, KantIKantVurderer kantIKantVurderer) {
        return t.compress((d1, d2) -> kantIKantVurderer.erKantIKant(DatoIntervallEntitet.fra(d1), DatoIntervallEntitet.fra(d2)), Boolean::equals, StandardCombinators::alwaysTrueForMatch);
    }

    private DatoIntervallEntitet bestemPeriode(UngdomsprogramPeriode it) {
        DatoIntervallEntitet periode = it.getPeriode();
        // TOM dato fra register kan være null som mapper til tidenes ende. Men vi lar likevel vilkåret ha en enkel
        // maksgrense foreløpig
        if (periode.getTomDato().equals(AbstractLocalDateInterval.TIDENES_ENDE)) {
            return DatoIntervallEntitet.fraOgMedTilOgMed(
                periode.getFomDato(), periode.getFomDato().plus(PeriodeKonstanter.MAKS_PERIODE));
        }

        return periode;
    }
}
