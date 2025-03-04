package no.nav.ung.sak.perioder;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoGrunnlag;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoer;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;

import java.util.Collections;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import static no.nav.ung.sak.domene.typer.tid.TidslinjeUtil.tilTidslinje;

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
        var søknadsperioder = søknadsperiodeRepository.hentGrunnlag(behandlingId).map(finnPeriodeHolder);

        if (søknadsperioder.isEmpty() || søknadsperioder.get().getStartdatoer().isEmpty()) {
            return Collections.emptyNavigableSet();
        } else {
            LocalDateTimeline<Boolean> ungdomsprogramtidslinje = ungdomsprogramPeriodeTjeneste.finnPeriodeTidslinje(behandlingId);
            var behandling = behandlingRepository.hentBehandling(behandlingId);
            var fagsak = behandling.getFagsak();
            final var fagsakTidslinje = new LocalDateTimeline<>(fagsak.getPeriode().getFomDato(), fagsak.getPeriode().getTomDato(), true);
            return ungdomsprogramtidslinje.intersection(fagsakTidslinje)
                .getLocalDateIntervals()
                .stream()
                .map(DatoIntervallEntitet::fra)
                .collect(Collectors.toCollection(TreeSet::new));
        }
    }


}
