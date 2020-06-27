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


    /**
     * Vilkårsperiode for april starter 1. mars pga historiske data i produksjon. Dette må migreres om datoen skal endres eller
     * diff i vilkårsperioder må håndteres.
     */
    public static final LocalDate APRIL_VILKÅRSPERIODE_FOM = LocalDate.of(2020, 3, 1);
    public static final LocalDate APRIL_VILKÅRSPERIODE_TOM = LocalDate.of(2020, 4, 30);
    public static final LocalDate MAI_VILKÅRSPERIODE_FOM = LocalDate.of(2020, 5, 1);
    public static final LocalDate MAI_VILKÅRSPERIODE_TOM = LocalDate.of(2020, 5, 31);
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

    /**
     *
     * Perioder for april og mai må vurderes samlet fordi disse har felles vilkårsperiode fra originalbehandling i tilnærmet alle søknader for april og mai.
     * På grunn av dette legges det til en av disse månedene om den andre er endret.
     *
     * @param endredeSøknadsmåneder Perioder/Måneder som er endret i overstyring av frisinn
     */
    private void leggTilAprilEllerMaiOmNødvendig(Set<DatoIntervallEntitet> endredeSøknadsmåneder) {
        if (inkludererMåned(endredeSøknadsmåneder, Month.APRIL) && !inkludererMåned(endredeSøknadsmåneder, Month.MAY)) {
            var mai = DatoIntervallEntitet.fraOgMedTilOgMed(MAI_VILKÅRSPERIODE_FOM, MAI_VILKÅRSPERIODE_TOM);
            endredeSøknadsmåneder.add(mai);
        } else if (inkludererMåned(endredeSøknadsmåneder, Month.MAY) && !inkludererMåned(endredeSøknadsmåneder, Month.APRIL)) {
            var april = DatoIntervallEntitet.fraOgMedTilOgMed(APRIL_VILKÅRSPERIODE_FOM, APRIL_VILKÅRSPERIODE_TOM);
            endredeSøknadsmåneder.add(april);
        }
    }

    private boolean inkludererMåned(Set<DatoIntervallEntitet> endredeSøknadsmåneder, Month måned) {
        return endredeSøknadsmåneder.stream().anyMatch(p -> YearMonth.from(p.getTomDato()).equals(YearMonth.of(2020, måned)));
    }

    private Set<DatoIntervallEntitet> mapTilHelePerioder(List<Periode> søknadsperioder) {
        return søknadsperioder.stream().map(this::mapTilFullSøknadsmåned).collect(Collectors.toSet());
    }

    private DatoIntervallEntitet mapTilFullSøknadsmåned(Periode periode) {
        LocalDate fomDato = periode.getFom();
        if (fomDato.getYear() == 2020 && (fomDato.getMonth().equals(Month.APRIL) || fomDato.getMonth().equals(Month.MARCH))) {
            return DatoIntervallEntitet.fraOgMedTilOgMed(APRIL_VILKÅRSPERIODE_FOM, APRIL_VILKÅRSPERIODE_TOM);
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
