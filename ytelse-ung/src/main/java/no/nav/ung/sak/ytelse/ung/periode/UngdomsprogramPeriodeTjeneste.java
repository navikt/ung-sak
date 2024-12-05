package no.nav.ung.sak.ytelse.ung.periode;

import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.DefaultKantIKantVurderer;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.KantIKantVurderer;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

@Dependent
public class UngdomsprogramPeriodeTjeneste {

    private final UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private static final KantIKantVurderer KANT_I_KANT_VURDERER = new DefaultKantIKantVurderer();

    @Inject
    public UngdomsprogramPeriodeTjeneste(UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository) {
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
    }


    public LocalDateTimeline<Boolean> finnPeriodeTidslinje(Long behandlingId) {
        var ungdomsprogramPeriodeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandlingId);
        return lagPeriodeTidslinje(ungdomsprogramPeriodeGrunnlag);
    }

    /**
     * Utleder tidslinje for endringer i perioder fra grunnlaget i forrige behandling og det aktive. I førstegangsbehandlinger vil alle perioder returnerers som endring.
     *
     * @param behandlingReferanse Behandlingreferanse
     * @return Tidslinje for endrede perioder
     */
    public LocalDateTimeline<Boolean> finnEndretPeriodeTidslinje(BehandlingReferanse behandlingReferanse) {
        var ungdomsprogramPeriodeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandlingReferanse.getBehandlingId());
        var periodeTidslinje = lagPeriodeTidslinje(ungdomsprogramPeriodeGrunnlag);
        var originaltGrunnlag = behandlingReferanse.getOriginalBehandlingId().flatMap(ungdomsprogramPeriodeRepository::hentGrunnlag);
        var initiellPeriodeTidslinje = lagPeriodeTidslinje(originaltGrunnlag);
        return initiellPeriodeTidslinje.crossJoin(periodeTidslinje, UngdomsprogramPeriodeTjeneste::erEndret)
            .filterValue(v -> v);
    }

    /**
     * Lager tidslinje for perioder der bruker deltar i ungdomsprogram basert på verdier fra et oppgitt periodegrunnlag
     *
     * @param ungdomsprogramPeriodeGrunnlag Ungdomsprogram-grunnlag med perioder der bruker er i ungdomsprogram
     * @return Tidslinje for perioder der bruker er i ungdomsprogram
     */
    private LocalDateTimeline<Boolean> lagPeriodeTidslinje(Optional<UngdomsprogramPeriodeGrunnlag> ungdomsprogramPeriodeGrunnlag) {
        return ungdomsprogramPeriodeGrunnlag.stream()
            .flatMap(gr -> gr.getUngdomsprogramPerioder().getPerioder().stream())
            .map(UngdomsprogramPeriode::getPeriode)
            .map(p -> new LocalDateTimeline<>(p.getFomDato(), p.getTomDato(), true))
            .reduce(LocalDateTimeline::crossJoin)
            .map(t -> komprimer(t))
            .orElse(LocalDateTimeline.empty());
    }

    private static LocalDateSegment<Boolean> erEndret(LocalDateInterval di, LocalDateSegment<Boolean> lhs, LocalDateSegment<Boolean> rhs) {
        return new LocalDateSegment<>(di, lhs == null || rhs == null || lhs.getValue().equals(rhs.getValue()));
    }


    private LocalDateTimeline<Boolean> komprimer(LocalDateTimeline<Boolean> t) {
        return t.compress((d1, d2) -> KANT_I_KANT_VURDERER.erKantIKant(DatoIntervallEntitet.fra(d1), DatoIntervallEntitet.fra(d2)), Boolean::equals, StandardCombinators::alwaysTrueForMatch);
    }

}
