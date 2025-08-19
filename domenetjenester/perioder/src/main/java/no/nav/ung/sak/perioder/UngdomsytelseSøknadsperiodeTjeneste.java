package no.nav.ung.sak.perioder;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoGrunnlag;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoer;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseSøktStartdato;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static no.nav.ung.sak.domene.typer.tid.TidslinjeUtil.tilTidslinje;

@Dependent
public class UngdomsytelseSøknadsperiodeTjeneste {

    private final UngdomsytelseStartdatoRepository startdatoRepository;
    private final UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste;
    private BehandlingRepository behandlingRepository;

    @Inject
    public UngdomsytelseSøknadsperiodeTjeneste(UngdomsytelseStartdatoRepository startdatoRepository, UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste, BehandlingRepository behandlingRepository) {

        this.startdatoRepository = startdatoRepository;
        this.ungdomsprogramPeriodeTjeneste = ungdomsprogramPeriodeTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    /**
     * Finner søknadsperioder som har kommet inn i denne behandlingen
     *
     * @param behandlingId BehandlingId
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
        var startdatoer = startdatoRepository.hentGrunnlag(behandlingId).map(finnPeriodeHolder);

        if (startdatoer.isEmpty() || startdatoer.get().getStartdatoer().isEmpty()) {
            return Collections.emptyNavigableSet();
        } else {
            var førsteRelevanteStartdato = startdatoer.map(UngdomsytelseStartdatoer::getStartdatoer)
                .stream()
                .flatMap(Collection::stream)
                .map(UngdomsytelseSøktStartdato::getStartdato)
                .min(Comparator.naturalOrder())
                .orElseThrow(() -> new IllegalStateException("Fant ingen startdatoer for behandlingId: " + behandlingId));
            LocalDateTimeline<Boolean> ungdomsprogramtidslinje = ungdomsprogramPeriodeTjeneste.finnPeriodeTidslinje(behandlingId);
            var behandling = behandlingRepository.hentBehandling(behandlingId);
            var fagsak = behandling.getFagsak();
            final var fagsakTidslinje = new LocalDateTimeline<>(fagsak.getPeriode().getFomDato(), fagsak.getPeriode().getTomDato(), true);
            return ungdomsprogramtidslinje.intersection(fagsakTidslinje)
                .getLocalDateIntervals()
                .stream()
                .filter(it -> it.getTomDato().isAfter(førsteRelevanteStartdato))
                .map(DatoIntervallEntitet::fra)
                .collect(Collectors.toCollection(TreeSet::new));
        }
    }


}
