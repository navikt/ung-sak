package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår;

import java.time.LocalDate;
import java.util.Collections;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPeriodiseringsFunksjon;

class MaksSøktePeriode implements VilkårsPeriodiseringsFunksjon {

    private SøktePerioder søktePerioder;

    MaksSøktePeriode(SøktePerioder søktePerioder) {
        this.søktePerioder = søktePerioder;
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> utledPeriode(Long behandlingId) {
        final NavigableSet<DatoIntervallEntitet> perioder = søktePerioder.utledPeriode(behandlingId);

        if (perioder.isEmpty()) {
            return Collections.emptyNavigableSet();
        } else {
            final var fom = perioder
                    .stream()
                    .map(DatoIntervallEntitet::getFomDato)
                    .min(LocalDate::compareTo)
                    .orElseThrow();

            final var tom = perioder
                    .stream()
                    .map(DatoIntervallEntitet::getTomDato)
                    .max(LocalDate::compareTo)
                    .orElseThrow();
            
            return Collections.unmodifiableNavigableSet(new TreeSet<>(Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))));
        }
    }
}
