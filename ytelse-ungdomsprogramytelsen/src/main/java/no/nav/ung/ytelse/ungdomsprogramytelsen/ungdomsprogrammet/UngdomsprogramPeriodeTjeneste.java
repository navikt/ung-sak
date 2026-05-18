package no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoGrunnlag;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoer;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.DefaultKantIKantVurderer;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.domene.typer.tid.KantIKantVurderer;
import no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.forbruktedager.FinnForbrukteDager;
import no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.forbruktedager.VurderAntallDagerResultat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class UngdomsprogramPeriodeTjeneste {

    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private static final KantIKantVurderer KANT_I_KANT_VURDERER = new DefaultKantIKantVurderer();

    @Inject
    public UngdomsprogramPeriodeTjeneste(UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository) {
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
    }

    public UngdomsprogramPeriodeTjeneste() {
        // CDI
    }

    public LocalDateTimeline<Boolean> finnPeriodeTidslinje(Long behandlingId) {
        var ungdomsprogramPeriodeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandlingId);
        return lagPeriodeTidslinje(ungdomsprogramPeriodeGrunnlag);
    }

    public LocalDateTimeline<Boolean> finnInitiellPeriodeTidslinje(Long behandlingId) {
        var ungdomsprogramPeriodeGrunnlag = ungdomsprogramPeriodeRepository.hentInitiell(behandlingId);
        return lagPeriodeTidslinje(ungdomsprogramPeriodeGrunnlag);
    }


    @WithSpan
    public VurderAntallDagerResultat finnVirkedagerTidslinje(Long behandlingId) {
        var tidslinje = finnPeriodeTidslinje(behandlingId);
        var harForlengetPeriode = finnHarForlengetPeriode(behandlingId);
        return FinnForbrukteDager.finnForbrukteDager(tidslinje, harForlengetPeriode);
    }

    public boolean finnHarForlengetPeriode(Long behandlingId) {
        return ungdomsprogramPeriodeRepository.hentGrunnlag(behandlingId)
            .map(gr -> gr.harForlengetPeriode())
            .orElse(false);
    }

    public Optional<LocalDate> finnPeriodeMaksDato(Long behandlingId) {
        return ungdomsprogramPeriodeRepository.hentGrunnlag(behandlingId)
            .flatMap(gr -> gr.getPeriodeMaksDato());
    }

    /**
     * Returnerer ungdomsprogramperiodene for en behandling der hver periode sin {@code tom}-dato
     * er kappet mot {@code periodeMaksDato} fra grunnlaget.
     *
     * <p>For hver periode brukes {@code min(opprinnelig tom, periodeMaksDato)}.
     * {@code periodeMaksDato} er allerede beregnet som en virkedag av ung-deltakelse-opplyser.
     * Dersom {@code periodeMaksDato} ikke er satt på grunnlaget beholdes opprinnelig {@code tom} uendret.
     * Perioder som ligger helt etter maksdato filtreres bort.
     *
     * @param behandlingId Behandling som det skal hentes perioder for
     * @return Perioder kappet mot maksdato, eller tom mengde dersom grunnlag mangler
     */
    public Set<UngdomsprogramPeriode> finnPerioderKappetMotMaksdato(Long behandlingId) {
        var grunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandlingId);
        if (grunnlag.isEmpty()) {
            return Set.of();
        }
        var perioder = grunnlag.get().getUngdomsprogramPerioder().getPerioder();
        var maksDato = grunnlag.get().getPeriodeMaksDato();
        if (maksDato.isEmpty()) {
            return perioder;
        }
        var kappetTom = maksDato.get();
        return perioder.stream()
            .filter(p -> !p.getPeriode().getFomDato().isAfter(kappetTom))
            .map(p -> {
                var opprinneligTom = p.getPeriode().getTomDato();
                var nyTom = opprinneligTom.isBefore(kappetTom) ? opprinneligTom : kappetTom;
                return new UngdomsprogramPeriode(p.getPeriode().getFomDato(), nyTom);
            })
            .collect(Collectors.toSet());
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

    public static LocalDateTimeline<Boolean> finnEndretTidslinje(Optional<UngdomsprogramPeriodeGrunnlag> ungdomsprogramPeriodeGrunnlag, Optional<UngdomsprogramPeriodeGrunnlag> originaltGrunnlag) {
        var periodeTidslinje = lagPeriodeTidslinje(ungdomsprogramPeriodeGrunnlag);
        var initiellPeriodeTidslinje = lagPeriodeTidslinje(originaltGrunnlag);
        return initiellPeriodeTidslinje.crossJoin(periodeTidslinje, UngdomsprogramPeriodeTjeneste::erEndret)
            .filterValue(v -> v);
    }

    public List<EndretDato> finnEndretStartdatoerFraOriginal(BehandlingReferanse behandlingReferanse) {
        var ungdomsprogramPeriodeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandlingReferanse.getBehandlingId());
        var originaltGrunnlag = behandlingReferanse.getOriginalBehandlingId().flatMap(ungdomsprogramPeriodeRepository::hentGrunnlag);
        if (originaltGrunnlag.isEmpty() || ungdomsprogramPeriodeGrunnlag.isEmpty()) {
            return List.of();
        }
        return finnEndretStartdatoer(ungdomsprogramPeriodeGrunnlag.get(), originaltGrunnlag.get());
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

    public static List<EndretDato> finnEndretStartdatoer(UngdomsprogramPeriodeGrunnlag andreGrunnlag, UngdomsprogramPeriodeGrunnlag førsteGrunnlag) {
        // Støtter kun en eller ingen periode her foreløpig
        var andrePeriode = andreGrunnlag.hentForEksaktEnPeriodeDersomFinnes();
        var førstePeriode = førsteGrunnlag.hentForEksaktEnPeriodeDersomFinnes();
        if (andrePeriode.isEmpty() || førstePeriode.isEmpty()) {
            return List.of();
        }
        var andreStartdato = andrePeriode.get().getFomDato();
        var førsteStartdato = førstePeriode.get().getFomDato();

        if (andreStartdato.equals(førsteStartdato)) {
            return List.of();
        }

        return List.of(new EndretDato(andreStartdato, førsteStartdato));
    }

    public static boolean harEndretStartdatoFraOppgittStartdatoer(UngdomsprogramPeriodeGrunnlag ungdomsprogramPeriodeGrunnlag, Optional<UngdomsytelseStartdatoGrunnlag> ungdomsytelseStartdatoGrunnlag) {
        // Støtter kun en eller ingen periode her foreløpig
        var gjelendePeriode = ungdomsprogramPeriodeGrunnlag.hentForEksaktEnPeriodeDersomFinnes();
        if (gjelendePeriode.isEmpty()) {
            return true;
        }
        var gjeldendeDato = gjelendePeriode.get().getFomDato();
        var oppgittStartdato = ungdomsytelseStartdatoGrunnlag.map(UngdomsytelseStartdatoGrunnlag::getOppgitteStartdatoer).map(UngdomsytelseStartdatoer::getStartdatoer)
            .orElse(Set.of())
            .iterator().next().getStartdato();
        return !gjeldendeDato.equals(oppgittStartdato);
    }

    public static List<EndretDato> finnEndretSluttdatoer(UngdomsprogramPeriodeGrunnlag andreGrunnlag, UngdomsprogramPeriodeGrunnlag førsteGrunnlag) {
        // Støtter kun en eller ingen periode her foreløpig
        var andrePeriode = andreGrunnlag.hentForEksaktEnPeriodeDersomFinnes();
        var førstePeriode = førsteGrunnlag.hentForEksaktEnPeriodeDersomFinnes();
        if (andrePeriode.isEmpty() || førstePeriode.isEmpty()) {
            return List.of();
        }
        var andreSluttdato = andrePeriode.get().getTomDato();
        var førsteSluttdato = førstePeriode.get().getTomDato();

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
        return new LocalDateSegment<>(di,
            lhs == null && rhs != null ||
                rhs == null && lhs != null ||
                rhs != null && !lhs.getValue().equals(rhs.getValue()));
    }


    private static LocalDateTimeline<Boolean> komprimer(LocalDateTimeline<Boolean> t) {
        return t.compress((d1, d2) -> KANT_I_KANT_VURDERER.erKantIKant(DatoIntervallEntitet.fra(d1), DatoIntervallEntitet.fra(d2)), Boolean::equals, StandardCombinators::alwaysTrueForMatch);
    }


    public record EndretDato(LocalDate nyDato, LocalDate forrigeDato) {
    }


}
