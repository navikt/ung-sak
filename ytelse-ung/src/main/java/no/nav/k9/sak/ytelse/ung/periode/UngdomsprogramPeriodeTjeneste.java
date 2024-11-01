package no.nav.k9.sak.ytelse.ung.periode;

import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.KantIKantVurderer;
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
        return lagPeriodeTidslinje(ungdomsprogramPeriodeGrunnlag, kantIKantVurderer);
    }

    /**
     * Utleder tidslinje for endringer i perioder fra grunnlaget i forrige behandling og det aktive. I førstegangsbehandlinger vil alle perioder returnerers som endring.
     *
     * @param behandlingReferanse Behandlingreferanse
     * @param kantIKantVurderer   Kant-i-kant-vurderer, vurderer om to perioder skal regnes for å være sammenhengende
     * @return Tidslinje for endrede perioder
     */
    public LocalDateTimeline<Boolean> finnEndretPeriodeTidslinje(BehandlingReferanse behandlingReferanse, KantIKantVurderer kantIKantVurderer) {
        var ungdomsprogramPeriodeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandlingReferanse.getBehandlingId());
        var periodeTidslinje = lagPeriodeTidslinje(ungdomsprogramPeriodeGrunnlag, kantIKantVurderer);
        var originaltGrunnlag = behandlingReferanse.getOriginalBehandlingId().flatMap(ungdomsprogramPeriodeRepository::hentGrunnlag);
        var initiellPeriodeTidslinje = lagPeriodeTidslinje(originaltGrunnlag, kantIKantVurderer);
        return initiellPeriodeTidslinje.crossJoin(periodeTidslinje, UngdomsprogramPeriodeTjeneste::erEndret)
            .filterValue(v -> v);
    }

    /**
     * Lager tidslinje for perioder der bruker deltar i ungdomsprogram basert på verdier fra et oppgitt periodegrunnlag
     *
     * @param ungdomsprogramPeriodeGrunnlag Ungdomsprogram-grunnlag med perioder der bruker er i ungdomsprogram
     * @param kantIKantVurderer             KantIKantVurderer som vurderer om en periode skal kunne slå sammen med en annen
     * @return Tidslinje for perioder der bruker er i ungdomsprogram
     */
    private LocalDateTimeline<Boolean> lagPeriodeTidslinje(Optional<UngdomsprogramPeriodeGrunnlag> ungdomsprogramPeriodeGrunnlag, KantIKantVurderer kantIKantVurderer) {
        return ungdomsprogramPeriodeGrunnlag.stream()
            .flatMap(gr -> gr.getUngdomsprogramPerioder().getPerioder().stream())
            .map(UngdomsprogramPeriode::getPeriode)
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

}
