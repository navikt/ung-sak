package no.nav.k9.sak.ytelse.frisinn.vilkår;

import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.perioder.VilkårsPeriodiseringsFunksjon;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.frisinn.mapper.FrisinnSøknadsperiodeMapper;

/**
 * Oppretter perioder per søknad som dekker heile søknadsmåneden
 */
class Søknadsperioder implements VilkårsPeriodiseringsFunksjon {

    private BehandlingRepository behandlingRepository;
    private UttakRepository uttakRepository;
    private UtledPerioderMedEndringTjeneste utledPerioderMedEndringTjeneste;

    Søknadsperioder(BehandlingRepository behandlingRepository, UttakRepository uttakRepository, UtledPerioderMedEndringTjeneste utledPerioderMedEndringTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.uttakRepository = uttakRepository;
        this.utledPerioderMedEndringTjeneste = utledPerioderMedEndringTjeneste;
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> utledPeriode(Long behandlingId) {
        var uttakAktivitet = uttakRepository.hentFastsattUttak(behandlingId);
        List<Periode> søknadsperioder = FrisinnSøknadsperiodeMapper.map(uttakAktivitet);

        var origUttakAktivitet = behandlingRepository.hentBehandling(behandlingId).getOriginalBehandling()
            .map(orig -> uttakRepository.hentFastsattUttak(orig.getId()));

        var origSøknadsperioder = origUttakAktivitet.map(FrisinnSøknadsperiodeMapper::map)
            .orElse(Collections.emptyList());

        Optional<Periode> nySøknadsperiode = finnNySøknadsperiode(origSøknadsperioder, søknadsperioder);

        if (nySøknadsperiode.isPresent()) {
            return Collections.unmodifiableNavigableSet(new TreeSet<>(Set.of(mapTilFullSøknadsmåned(nySøknadsperiode.get()))));
        } else {
            // Revurdering
            var søknadsmåneder = mapTilHelePerioder(søknadsperioder);
            var endredeMånederIRevurdering = utledPerioderMedEndringTjeneste.finnPeriodeMedEndring(behandlingId);
            var endredeSøknadsmåneder = søknadsmåneder.stream()
                .filter(it -> endredeMånederIRevurdering.stream().anyMatch(endret -> endret.overlapper(it)))
                .collect(Collectors.toSet());
            leggTilAprilEllerMaiOmNødvendig(endredeSøknadsmåneder);
            return Collections.unmodifiableNavigableSet(new TreeSet<>(endredeSøknadsmåneder));
        }

    }

    private void leggTilAprilEllerMaiOmNødvendig(Set<DatoIntervallEntitet> endredeSøknadsmåneder) {
        if (inkludererMåned(endredeSøknadsmåneder, Month.APRIL) && !inkludererMåned(endredeSøknadsmåneder, Month.MAY)) {
            var mai = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2020, 5, 1), LocalDate.of(2020, 5, 31));
            endredeSøknadsmåneder.add(mai);
        } else if (inkludererMåned(endredeSøknadsmåneder, Month.MAY) && !inkludererMåned(endredeSøknadsmåneder, Month.APRIL)) {
            var april = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2020, 3, 30), LocalDate.of(2020, 4, 30));
            endredeSøknadsmåneder.add(april);
        }
    }

    private boolean inkludererMåned(Set<DatoIntervallEntitet> endredeSøknadsmåneder, Month måned) {
        return endredeSøknadsmåneder.stream().anyMatch(p -> YearMonth.from(p.getTomDato()).equals(YearMonth.of(2020, måned)));
    }

    @NotNull
    private Set<DatoIntervallEntitet> mapTilHelePerioder(List<Periode> søknadsperioder) {
        return søknadsperioder.stream().map(this::mapTilFullSøknadsmåned).collect(Collectors.toSet());
    }

    @NotNull
    private DatoIntervallEntitet mapTilFullSøknadsmåned(Periode periode) {
        LocalDate fomDato = periode.getFom();
        if (fomDato.getYear() == 2020 && (fomDato.getMonth().equals(Month.APRIL) || fomDato.getMonth().equals(Month.MARCH))) {
            return DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2020, 3, 1), LocalDate.of(2020, 4, 30));
        } else {
            return DatoIntervallEntitet.fraOgMedTilOgMed(fomDato.withDayOfMonth(1), fomDato.with(TemporalAdjusters.lastDayOfMonth()));
        }
    }

    private Optional<Periode> finnNySøknadsperiode(List<Periode> perioderOrig, List<Periode> perioder) {
        if (perioder.size() > perioderOrig.size()) {
            return perioder.stream()
                .sorted(Comparator.comparing(Periode::getFom, Comparator.reverseOrder()))
                .findFirst();
        }
        return Optional.empty();
    }

}
