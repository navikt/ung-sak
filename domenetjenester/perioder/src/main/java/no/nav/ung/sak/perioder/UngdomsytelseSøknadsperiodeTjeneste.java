package no.nav.ung.sak.perioder;

import static no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_ENDE;
import static no.nav.ung.sak.domene.typer.tid.TidslinjeUtil.tilTidslinje;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoGrunnlag;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoer;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseSøktStartdato;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;

@Dependent
public class UngdomsytelseSøknadsperiodeTjeneste {

    private final UngdomsytelseStartdatoRepository søknadsperiodeRepository;
    private final UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste;
    private BehandlingRepository behandlingRepository;

    @Inject
    public UngdomsytelseSøknadsperiodeTjeneste(UngdomsytelseStartdatoRepository søknadsperiodeRepository, UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste, BehandlingRepository behandlingRepository) {

        this.søknadsperiodeRepository = søknadsperiodeRepository;
        this.ungdomsprogramPeriodeTjeneste = ungdomsprogramPeriodeTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    /**
     * Finner søknadsperioder som har kommet inn i denne behandlingen
     *
     * @param behandlingId      BehandlingId
     * @return Relevante søknadsperioder for denne behandlingen
     */
    public NavigableSet<DatoIntervallEntitet> utledPeriode(Long behandlingId) {
        return finnPerioder(behandlingId, UngdomsytelseStartdatoGrunnlag::getRelevanteStartdatoer);
    }

    public LocalDateTimeline<Boolean> utledTidslinje(Long behandlingId) {
        return tilTidslinje(finnPerioder(behandlingId, UngdomsytelseStartdatoGrunnlag::getRelevanteStartdatoer));
    }

    /**
     * Finner alle perioder som har kommet inn på alle tidligere behandlinger.
     *
     * @param behandlingId behandlingid
     * @return Alle perioder fra alle behandlinger
     */
    public NavigableSet<DatoIntervallEntitet> utledFullstendigPeriode(Long behandlingId) {
        return finnPerioder(behandlingId, UngdomsytelseStartdatoGrunnlag::getOppgitteStartdatoer);
    }

    private NavigableSet<DatoIntervallEntitet> finnPerioder(Long behandlingId,
                                                            Function<UngdomsytelseStartdatoGrunnlag, UngdomsytelseStartdatoer> finnPeriodeHolder) {
        var søknadsperioder = søknadsperiodeRepository.hentGrunnlag(behandlingId).map(finnPeriodeHolder);

        if (søknadsperioder.isEmpty() || søknadsperioder.get().getStartdatoer().isEmpty()) {
            return Collections.emptyNavigableSet();
        } else {
            LocalDateTimeline<Boolean> ungdomsprogramtidslinje = ungdomsprogramPeriodeTjeneste.finnPeriodeTidslinje(behandlingId);
            final var søkteDatoer = søknadsperioder.get().getStartdatoer();
            var startdatoer = søkteDatoer.stream().map(UngdomsytelseSøktStartdato::getStartdato)
                .sorted()
                .toList();

            var behandling = behandlingRepository.hentBehandling(behandlingId);

            var fagsak = behandling.getFagsak();

            final NavigableSet<DatoIntervallEntitet> resultatPerioder = new TreeSet<>();

            final var ungdomsprogramperioder = ungdomsprogramtidslinje.getLocalDateIntervals();
            for (var startDato: startdatoer) {
                final var sluttdatoForPeriodeMedMinstAvstandTilStartdato = ungdomsprogramperioder.stream()
                    .filter(p -> !p.getFomDato().isBefore(startDato)) // Støtter i første omgang kun endring av startdato fram i tid
                    .min(Comparator.comparing(i1 -> startDato.until(i1.getFomDato().plusDays(1), ChronoUnit.DAYS)))
                    .map(LocalDateInterval::getTomDato)
                    .map(d -> begrensTomDato(fagsak, d));
                if (sluttdatoForPeriodeMedMinstAvstandTilStartdato.isPresent() && harIkkeBruktPeriode(resultatPerioder, sluttdatoForPeriodeMedMinstAvstandTilStartdato)) {
                    resultatPerioder.add(DatoIntervallEntitet.fraOgMedTilOgMed(startDato, sluttdatoForPeriodeMedMinstAvstandTilStartdato.get()));
                }

            }

            return resultatPerioder;
        }
    }

    private static boolean harIkkeBruktPeriode(NavigableSet<DatoIntervallEntitet> resultatPerioder, Optional<LocalDate> sluttdato) {
        return resultatPerioder.stream().noneMatch(p -> p.getTomDato().equals(sluttdato.get()));
    }

    private static LocalDate begrensTomDato(Fagsak fagsak, LocalDate sluttDato) {
        return sluttDato.equals(TIDENES_ENDE) ? fagsak.getPeriode().getTomDato() : sluttDato;
    }


}
