package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPeriodiseringsFunksjon;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperioder;

class MaksSøktePeriode implements VilkårsPeriodiseringsFunksjon {

    private SøknadsperiodeRepository søknadsperiodeRepository;

    MaksSøktePeriode(SøknadsperiodeRepository søknadsperiodeRepository) {
        this.søknadsperiodeRepository = søknadsperiodeRepository;
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> utledPeriode(Long behandlingId) {
        var søknadsperioder = søknadsperiodeRepository.hentGrunnlag(behandlingId).map(SøknadsperiodeGrunnlag::getOppgitteSøknadsperioder);

        if (søknadsperioder.isEmpty() || søknadsperioder.get().getPerioder().isEmpty()) {
            return Collections.emptyNavigableSet();
        } else {
            var perioder = søknadsperioder.get().getPerioder();
            var minDato = perioder.stream()
                .map(Søknadsperioder::getPerioder)
                .flatMap(Collection::stream)
                .map(Søknadsperiode::getPeriode)
                .map(DatoIntervallEntitet::getFomDato)
                .min(LocalDate::compareTo)
                .orElseThrow();
            var maxDato = perioder.stream()
                .map(Søknadsperioder::getPerioder)
                .flatMap(Collection::stream)
                .map(Søknadsperiode::getPeriode)
                .map(DatoIntervallEntitet::getTomDato)
                .max(LocalDate::compareTo)
                .orElseThrow();

            return Collections.unmodifiableNavigableSet(new TreeSet<>(Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(minDato, maxDato))));
        }
    }
}
