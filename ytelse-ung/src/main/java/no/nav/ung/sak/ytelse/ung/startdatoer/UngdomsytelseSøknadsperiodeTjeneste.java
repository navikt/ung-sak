package no.nav.ung.sak.ytelse.ung.startdatoer;

import static no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_ENDE;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.ytelse.ung.periode.UngdomsprogramPeriodeTjeneste;

import java.time.LocalDate;
import java.util.Collections;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

@Dependent
public class UngdomsytelseSøknadsperiodeTjeneste {

    private final UngdomsytelseSøknadsperiodeRepository søknadsperiodeRepository;
    private final UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste;
    private BehandlingRepository behandlingRepository;

    @Inject
    public UngdomsytelseSøknadsperiodeTjeneste(UngdomsytelseSøknadsperiodeRepository søknadsperiodeRepository, UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste, BehandlingRepository behandlingRepository) {

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
        return finnPerioder(behandlingId, UngdomsytelseSøknadGrunnlag::getRelevantSøknader);
    }

    /**
     * Finner alle perioder som har kommet inn på alle tidligere behandlinger.
     *
     * @param behandlingId behandlingid
     * @return Alle perioder fra alle behandlinger
     */
    public NavigableSet<DatoIntervallEntitet> utledFullstendigPeriode(Long behandlingId) {
        return finnPerioder(behandlingId, UngdomsytelseSøknadGrunnlag::getOppgitteSøknader);
    }

    private NavigableSet<DatoIntervallEntitet> finnPerioder(Long behandlingId,
                                                            Function<UngdomsytelseSøknadGrunnlag, UngdomsytelseSøknader> finnPeriodeHolder) {
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
