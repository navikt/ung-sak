package no.nav.ung.sak.ungdomsprogram;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoGrunnlag;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoer;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.DefaultKantIKantVurderer;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.KantIKantVurderer;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.ungdomsprogram.forbruktedager.FinnForbrukteDager;
import no.nav.ung.sak.ungdomsprogram.forbruktedager.VurderAntallDagerResultat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Dependent
public class UngdomsprogramPeriodeTjeneste {

    private final UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private final UngdomsytelseStartdatoRepository ungdomsytelseStartdatoRepository;
    private static final KantIKantVurderer KANT_I_KANT_VURDERER = new DefaultKantIKantVurderer();

    @Inject
    public UngdomsprogramPeriodeTjeneste(UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository, UngdomsytelseStartdatoRepository ungdomsytelseStartdatoRepository) {
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
        this.ungdomsytelseStartdatoRepository = ungdomsytelseStartdatoRepository;
    }


    public LocalDateTimeline<Boolean> finnPeriodeTidslinje(Long behandlingId) {
        var ungdomsprogramPeriodeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandlingId);
        return lagPeriodeTidslinje(ungdomsprogramPeriodeGrunnlag);
    }

    @WithSpan
    public VurderAntallDagerResultat finnVirkedagerTidslinje(Long behandlingId) {
        var tidslinje = finnPeriodeTidslinje(behandlingId);
        return FinnForbrukteDager.finnForbrukteDager(tidslinje);
    }

    /**
     * Utleder tidslinje for endringer i perioder fra grunnlaget i forrige behandling og det aktive. I førstegangsbehandlinger vil alle perioder returnerers som endring.
     *
     * @param behandlingReferanse Behandlingreferanse
     * @return Tidslinje for endrede perioder
     */
    public LocalDateTimeline<Boolean> finnEndretPeriodeTidslinjeFraOriginal(BehandlingReferanse behandlingReferanse) {
        var ungdomsprogramPeriodeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandlingReferanse.getBehandlingId());
        var periodeTidslinje = lagPeriodeTidslinje(ungdomsprogramPeriodeGrunnlag);
        var originaltGrunnlag = behandlingReferanse.getOriginalBehandlingId().flatMap(ungdomsprogramPeriodeRepository::hentGrunnlag);
        var initiellPeriodeTidslinje = lagPeriodeTidslinje(originaltGrunnlag);
        return initiellPeriodeTidslinje.crossJoin(periodeTidslinje, UngdomsprogramPeriodeTjeneste::erEndret)
            .filterValue(v -> v);
    }

    public LocalDateTimeline<Boolean> finnEndretPeriodeTidslinje(UUID fraReferanse, UUID tilReferanse) {
        var ungdomsprogramPeriodeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlagFraGrunnlagsReferanse(tilReferanse);
        var originaltGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlagFraGrunnlagsReferanse(fraReferanse);
        return finnEndretTidslinje(ungdomsprogramPeriodeGrunnlag, originaltGrunnlag);
    }

    public static LocalDateTimeline<Boolean> finnEndretTidslinje(Optional<UngdomsprogramPeriodeGrunnlag> ungdomsprogramPeriodeGrunnlag, Optional<UngdomsprogramPeriodeGrunnlag> originaltGrunnlag) {
        var periodeTidslinje = lagPeriodeTidslinje(ungdomsprogramPeriodeGrunnlag);
        var initiellPeriodeTidslinje = lagPeriodeTidslinje(originaltGrunnlag);
        return initiellPeriodeTidslinje.crossJoin(periodeTidslinje, UngdomsprogramPeriodeTjeneste::erEndret)
            .filterValue(v -> v);
    }

    public List<EndretDato> finnEndretStartdatoer(UUID førsteReferanse, UUID andreReferanse) {
        var andreGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlagFraGrunnlagsReferanse(andreReferanse).orElseThrow(() -> new IllegalStateException("Forventet å finne grunnlag for referanse: " + andreReferanse));
        var førsteGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlagFraGrunnlagsReferanse(førsteReferanse).orElseThrow(() -> new IllegalStateException("Forventet å finne grunnlag for referanse: " + førsteReferanse));
        return finnEndretStartdatoer(andreGrunnlag, førsteGrunnlag);
    }

    public static List<EndretDato> finnEndretStartdatoer(UngdomsprogramPeriodeGrunnlag andreGrunnlag, UngdomsprogramPeriodeGrunnlag førsteGrunnlag) {
        // Støtter kun en periode her foreløpig
        var andrePeriode = andreGrunnlag.hentForEksaktEnPeriode();
        var førstePeriode = førsteGrunnlag.hentForEksaktEnPeriode();
        var andreStartdato = andrePeriode.getFomDato();
        var førsteStartdato = førstePeriode.getFomDato();

        if (andreStartdato.equals(førsteStartdato)) {
            return List.of();
        }

        return List.of(new EndretDato(andreStartdato, førsteStartdato));
    }

    public static List<EndretDato> finnEndretStartdatoFraOppgittStartdatoer(UngdomsprogramPeriodeGrunnlag ungdomsprogramPeriodeGrunnlag, Optional<UngdomsytelseStartdatoGrunnlag> ungdomsytelseStartdatoGrunnlag) {
        // Støtter kun en periode her foreløpig
        var gjelendePeriode = ungdomsprogramPeriodeGrunnlag.hentForEksaktEnPeriode();
        var gjeldendeDato = gjelendePeriode.getFomDato();
        var oppgittStartdato = ungdomsytelseStartdatoGrunnlag.map(UngdomsytelseStartdatoGrunnlag::getOppgitteStartdatoer).map(UngdomsytelseStartdatoer::getStartdatoer)
            .orElse(Set.of())
            .iterator().next().getStartdato();
        if (oppgittStartdato.equals(gjeldendeDato)) {
            return List.of();
        }

        return List.of(new EndretDato(gjeldendeDato, oppgittStartdato));
    }

    public List<EndretDato> finnEndretSluttdatoer(UUID førsteReferanse, UUID andreReferanse) {
        var andreGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlagFraGrunnlagsReferanse(andreReferanse).orElseThrow(() -> new IllegalStateException("Forventet å finne grunnlag for referanse: " + andreReferanse));
        var førsteGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlagFraGrunnlagsReferanse(førsteReferanse).orElseThrow(() -> new IllegalStateException("Forventet å finne grunnlag for referanse: " + førsteReferanse));
        return finnEndretSluttdatoer(andreGrunnlag, førsteGrunnlag);
    }

    public static List<EndretDato> finnEndretSluttdatoer(UngdomsprogramPeriodeGrunnlag andreGrunnlag, UngdomsprogramPeriodeGrunnlag førsteGrunnlag) {
        // Støtter kun en periode her foreløpig
        var andrePeriode = andreGrunnlag.hentForEksaktEnPeriode();
        var førstePeriode = førsteGrunnlag.hentForEksaktEnPeriode();
        var andreSluttdato = andrePeriode.getTomDato();
        var førsteSluttdato = førstePeriode.getTomDato();

        if (andreSluttdato.equals(førsteSluttdato)) {
            return List.of();
        }

        return List.of(new EndretDato(andreSluttdato, førsteSluttdato));
    }


    /**
     * Lager tidslinje for perioder der bruker deltar i ungdomsprogram basert på verdier fra et oppgitt periodegrunnlag
     *
     * @param ungdomsprogramPeriodeGrunnlag Ungdomsprogram-grunnlag med perioder der bruker er i ungdomsprogram
     * @return Tidslinje for perioder der bruker er i ungdomsprogram
     */
    public static LocalDateTimeline<Boolean> lagPeriodeTidslinje(Optional<UngdomsprogramPeriodeGrunnlag> ungdomsprogramPeriodeGrunnlag) {
        return ungdomsprogramPeriodeGrunnlag.stream()
            .flatMap(gr -> gr.getUngdomsprogramPerioder().getPerioder().stream())
            .map(UngdomsprogramPeriode::getPeriode)
            .map(p -> new LocalDateTimeline<>(p.getFomDato(), p.getTomDato(), true))
            .reduce(LocalDateTimeline::crossJoin)
            .map(UngdomsprogramPeriodeTjeneste::komprimer)
            .orElse(LocalDateTimeline.empty());
    }

    public static LocalDateSegment<Boolean> erEndret(LocalDateInterval di, LocalDateSegment<Boolean> lhs, LocalDateSegment<Boolean> rhs) {
        return new LocalDateSegment<>(di, lhs == null || rhs == null || !lhs.getValue().equals(rhs.getValue()));
    }


    private static LocalDateTimeline<Boolean> komprimer(LocalDateTimeline<Boolean> t) {
        return t.compress((d1, d2) -> KANT_I_KANT_VURDERER.erKantIKant(DatoIntervallEntitet.fra(d1), DatoIntervallEntitet.fra(d2)), Boolean::equals, StandardCombinators::alwaysTrueForMatch);
    }


    public record EndretDato(LocalDate nyDato, LocalDate forrigeDato) {
    }


}
