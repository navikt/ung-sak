package no.nav.ung.sak.perioder;

import static no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_ENDE;

import java.time.LocalDate;
import java.util.Collections;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
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
                .toList();

            var behandling = behandlingRepository.hentBehandling(behandlingId);

            var fagsak = behandling.getFagsak();

            TreeSet<DatoIntervallEntitet> relvanteUngdomsprogramperioder = ungdomsprogramtidslinje.stream()
                .filter(it -> startdatoer.contains(it.getFom()))
                .map(it -> DatoIntervallEntitet.fraOgMedTilOgMed(it.getFom(), begrensTomDato(it, fagsak)))
                .collect(Collectors.toCollection(TreeSet::new));

            return relvanteUngdomsprogramperioder;
        }
    }

    private static LocalDate begrensTomDato(LocalDateSegment<Boolean> it, Fagsak fagsak) {
        return it.getTom().equals(TIDENES_ENDE) ? fagsak.getPeriode().getTomDato() : it.getTom();
    }


}
