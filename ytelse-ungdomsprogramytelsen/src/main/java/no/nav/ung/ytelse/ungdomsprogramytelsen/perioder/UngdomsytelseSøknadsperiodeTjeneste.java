package no.nav.ung.ytelse.ungdomsprogramytelsen.perioder;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.StartdatoGrunnlag;
import no.nav.ung.sak.behandlingslager.behandling.startdato.StartdatoRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.Startdatoer;
import no.nav.ung.sak.behandlingslager.behandling.startdato.SøktStartdato;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.UngdomsprogramPeriodeTjeneste;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static no.nav.ung.sak.domene.typer.tid.TidslinjeUtil.tilTidslinje;

@Dependent
public class UngdomsytelseSøknadsperiodeTjeneste {

    private final StartdatoRepository startdatoRepository;
    private final UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste;
    private BehandlingRepository behandlingRepository;

    @Inject
    public UngdomsytelseSøknadsperiodeTjeneste(StartdatoRepository startdatoRepository, UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste, BehandlingRepository behandlingRepository) {
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
        return finnPerioder(behandlingId, StartdatoGrunnlag::getRelevanteStartdatoer);
    }

    public LocalDateTimeline<Boolean> utledTidslinje(Long behandlingId) {
        return tilTidslinje(finnPerioder(behandlingId, StartdatoGrunnlag::getRelevanteStartdatoer));
    }

    /**
     * Finner alle perioder som har kommet inn på alle tidligere behandlinger.
     *
     * @param behandlingId behandlingid
     * @return Alle perioder fra alle behandlinger
     */
    public NavigableSet<DatoIntervallEntitet> utledFullstendigPeriode(Long behandlingId) {
        return finnPerioder(behandlingId, StartdatoGrunnlag::getOppgitteStartdatoer);
    }

    private NavigableSet<DatoIntervallEntitet> finnPerioder(Long behandlingId,
                                                            Function<StartdatoGrunnlag, Startdatoer> finnPeriodeHolder) {
        var startdatoer = startdatoRepository.hentGrunnlag(behandlingId).map(finnPeriodeHolder);

        if (startdatoer.isEmpty() || startdatoer.get().getStartdatoer().isEmpty()) {
            return Collections.emptyNavigableSet();
        } else {
            var førsteRelevanteStartdato = startdatoer.map(Startdatoer::getStartdatoer)
                .stream()
                .flatMap(Collection::stream)
                .map(SøktStartdato::getStartdato)
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
