package no.nav.k9.sak.ytelse.frisinn.vilkår;

import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.uttak.repo.Søknadsperiode;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitetPeriode;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.perioder.VilkårsPeriodiseringsFunksjon;
import no.nav.k9.sak.ytelse.frisinn.mapper.FrisinnMapper;

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
        var uttakPerioder = uttakRepository.hentFastsattUttak(behandlingId).getPerioder();
        var søknadsperioder = sammenslåPåMåneder(behandlingId, uttakPerioder);
        if (søknadsperioder.isEmpty()) {
            return Collections.emptyNavigableSet();
        } else {
            Set<DatoIntervallEntitet> søknadsmåneder = søknadsperioder.stream()
                .map(DatoIntervallEntitet::getFomDato)
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

    private List<DatoIntervallEntitet> sammenslåPåMåneder(Long behandlingId, Set<UttakAktivitetPeriode> søknadsperioder) {
        var fastsattUttak = uttakRepository.hentFastsattUttak(behandlingId);
        var måneder = FrisinnMapper.finnMåneder(fastsattUttak).stream()
            .map(it -> DatoIntervallEntitet.fraOgMedTilOgMed(it.getFom(), it.getTom()))
            .collect(Collectors.toList());

        var søknadsperiodeTilMåned = søknadsperioder.stream()
            .collect(Collectors.groupingBy(søknadsperiode -> hentMåned(søknadsperiode.getPeriode(), måneder)));
        // TODO (essv): Gjør dette smartere
        var sammenslåttePerioder = søknadsperiodeTilMåned.values().stream()
            .map(perioder -> {
                var fom = perioder.stream()
                    .map(it -> it.getPeriode().getFomDato())
                    .sorted()
                    .findFirst()
                    .orElseThrow();
                var tom = perioder.stream()
                    .map(it -> it.getPeriode().getTomDato())
                    .max(Comparator.naturalOrder())
                    .orElseThrow();
                return DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
            })
            .collect(Collectors.toList());
        return sammenslåttePerioder;
    }

    private DatoIntervallEntitet hentMåned(DatoIntervallEntitet søknadsperiode, List<DatoIntervallEntitet> måneder) {
        return måneder.stream()
            .filter(søknadsperiode::overlapper)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Søknadsperiode må være innenfor måneder"));
    }


}
