package no.nav.k9.sak.ytelse.frisinn.vilkår;

import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.perioder.VilkårsPeriodiseringsFunksjon;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.frisinn.mapper.FrisinnSøknadsperiodeMapper;

/**
 * Oppretter perioder per søknad som dekker heile søknadsmåneden
 */
class Søknadsperioder implements VilkårsPeriodiseringsFunksjon {

    Søknadsperioder(UttakRepository uttakRepository) {
        this.uttakRepository = uttakRepository;
    }

    private UttakRepository uttakRepository;

    @Override
    public NavigableSet<DatoIntervallEntitet> utledPeriode(Long behandlingId) {
        var uttakAktivitet = uttakRepository.hentFastsattUttak(behandlingId);
        List<Periode> søknadsperioder = FrisinnSøknadsperiodeMapper.map(uttakAktivitet);
        if (søknadsperioder.isEmpty()) {
            return Collections.emptyNavigableSet();
        } else {
            Set<DatoIntervallEntitet> søknadsmåneder = søknadsperioder.stream()
                .map(Periode::getFom)
                .map(fomDato -> {
                    if (fomDato.getMonth().equals(Month.APRIL) || fomDato.getMonth().equals(Month.MARCH)) {
                        return DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2020, 3, 30), LocalDate.of(2020, 4, 30));
                    } else {
                        return DatoIntervallEntitet.fraOgMedTilOgMed(fomDato.withDayOfMonth(1), fomDato.with(TemporalAdjusters.lastDayOfMonth()));
                    }
                }).collect(Collectors.toSet());
            return Collections.unmodifiableNavigableSet(new TreeSet<>(søknadsmåneder));
        }
    }

}
