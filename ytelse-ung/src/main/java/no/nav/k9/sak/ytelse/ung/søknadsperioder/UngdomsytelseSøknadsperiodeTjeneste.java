package no.nav.k9.sak.ytelse.ung.søknadsperioder;

import java.util.Collections;
import java.util.NavigableSet;

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

    public NavigableSet<DatoIntervallEntitet> utledPeriode(Long behandlingId) {
        var søknadsperioder = søknadsperiodeRepository.hentGrunnlag(behandlingId)
            .map(UngdomsytelseSøknadsperiodeGrunnlag::getOppgitteSøknadsperioder);

        if (søknadsperioder.isEmpty() || søknadsperioder.get().getPerioder().isEmpty()) {
            return Collections.emptyNavigableSet();
        } else {
            final var søknadsperioders = søknadsperioder.get().getPerioder();
            var perioder = søknadsperioders.stream().flatMap(p -> p.getPerioder().stream())
                .map(UngdomsytelseSøknadsperiode::getPeriode)
                .toList();
            var tidslinje = TidslinjeUtil.tilTidslinjeMedMuligOverlapp(perioder);
            return TidslinjeUtil.tilDatoIntervallEntiteter(tidslinje);
        }
    }


}
