package no.nav.k9.sak.ytelse.ung.søknadsperioder;

import java.util.Collections;
import java.util.NavigableSet;
import java.util.function.Function;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;

@Dependent
public class UngdomsytelseSøknadsperiodeTjeneste {

    private final UngdomsytelseSøknadsperiodeRepository søknadsperiodeRepository;

    @Inject
    public UngdomsytelseSøknadsperiodeTjeneste(UngdomsytelseSøknadsperiodeRepository søknadsperiodeRepository) {

        this.søknadsperiodeRepository = søknadsperiodeRepository;
    }

    /** Finner søknadsperioder som har kommet inn i denne behandlingen
     * @param behandlingId BehandlingId
     * @return Relevante søknadsperioder for denne behandlingen
     */
    public NavigableSet<DatoIntervallEntitet> utledPeriode(Long behandlingId) {
        return finnPerioder(behandlingId, UngdomsytelseSøknadsperiodeGrunnlag::getRelevantSøknadsperioder);
    }

    /** Finner alle perioder som har kommet inn på alle tidligere behandlinger.
     * @param behandlingId behandlingid
     * @return Alle perioder fra alle behandlinger
     */
    public NavigableSet<DatoIntervallEntitet> utledFullstendigPeriode(Long behandlingId) {
        return finnPerioder(behandlingId, UngdomsytelseSøknadsperiodeGrunnlag::getOppgitteSøknadsperioder);
    }

    private NavigableSet<DatoIntervallEntitet> finnPerioder(Long behandlingId, Function<UngdomsytelseSøknadsperiodeGrunnlag, UngdomsytelseSøknadsperioderHolder> finnPeriodeHolder) {
        var søknadsperioder = søknadsperiodeRepository.hentGrunnlag(behandlingId).map(finnPeriodeHolder);

        if (søknadsperioder.isEmpty() || søknadsperioder.get().getPerioder().isEmpty()) {
            return Collections.emptyNavigableSet();
        } else {
            final var søknadsperioders = søknadsperioder.get().getPerioder();
            var perioder = søknadsperioders.stream().flatMap(p -> p.getPerioder().stream())
                .map(UngdomsytelseSøknadsperiode::getPeriode)
                .toList();
            var tidslinje = TidslinjeUtil.tilTidslinje(perioder);
            return TidslinjeUtil.tilDatoIntervallEntiteter(tidslinje);
        }
    }


}
