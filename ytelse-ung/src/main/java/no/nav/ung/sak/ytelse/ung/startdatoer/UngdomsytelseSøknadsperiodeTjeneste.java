package no.nav.ung.sak.ytelse.ung.startdatoer;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.ytelse.ung.periode.UngdomsprogramPeriodeTjeneste;

import java.util.Collections;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

@Dependent
public class UngdomsytelseSøknadsperiodeTjeneste {

    private final UngdomsytelseSøknadsperiodeRepository søknadsperiodeRepository;
    private final UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste;

    @Inject
    public UngdomsytelseSøknadsperiodeTjeneste(UngdomsytelseSøknadsperiodeRepository søknadsperiodeRepository, UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste) {

        this.søknadsperiodeRepository = søknadsperiodeRepository;
        this.ungdomsprogramPeriodeTjeneste = ungdomsprogramPeriodeTjeneste;
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

            TreeSet<DatoIntervallEntitet> relvanteUngdomsprogramperioder = ungdomsprogramtidslinje.stream()
                .filter(it -> startdatoer.contains(it.getFom()))
                .map(it -> DatoIntervallEntitet.fraOgMedTilOgMed(it.getFom(), it.getTom()))
                .collect(Collectors.toCollection(TreeSet::new));

            return relvanteUngdomsprogramperioder;
        }
    }


}
