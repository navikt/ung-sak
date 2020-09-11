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

        var origUttakAktivitet = behandlingRepository.hentBehandling(behandlingId).getOriginalBehandlingId()
            .map(orig -> uttakRepository.hentFastsattUttak(orig));

        var origSøknadsperioder = origUttakAktivitet.map(FrisinnSøknadsperiodeMapper::map)
            .orElse(Collections.emptyList());

        Optional<Periode> nySøknadsperiode = finnNySøknadsperiode(origSøknadsperioder, søknadsperioder);

        if (nySøknadsperiode.isPresent()) {
            return Collections.unmodifiableNavigableSet(new TreeSet<>(mapTilHelePerioder(List.of(nySøknadsperiode.get()))));
        } else {
            // Revurdering
            var søknadsmåneder = mapTilHelePerioder(søknadsperioder);
            var endredeMånederIRevurdering = utledPerioderMedEndringTjeneste.finnPeriodeMedEndring(behandlingId);
            var endredeSøknadsmåneder = søknadsmåneder.stream()
                .filter(it -> endredeMånederIRevurdering.stream().anyMatch(endret -> endret.overlapper(it)))
                .collect(Collectors.toSet());
            return Collections.unmodifiableNavigableSet(new TreeSet<>(endredeSøknadsmåneder));
        }

    }

    private Set<DatoIntervallEntitet> mapTilHelePerioder(List<Periode> søknadsperioder) {
        Set<DatoIntervallEntitet> fulleMåneder = søknadsperioder.stream().map(this::mapTilFullSøknadsmåned).collect(Collectors.toSet());
        return slåSammenAprilOgMai(fulleMåneder);
    }

    /**
     *
     * Perioder for april og mai må vurderes samlet fordi disse har felles vilkårsperiode fra originalbehandling i alle søknader som inkluderer både april og mai.
     * På grunn av dette slås april og mai sammen til en periode (vilkårsperiode). Se også {@link IkkeKantIKantVurderer#erKantIKant}
     *
     * @param fulleMåneder Søknadsperioder konvertert til fulle søknadsmåneder
     * @return Perioder der april og mai er slått sammen
     */
    private Set<DatoIntervallEntitet> slåSammenAprilOgMai(Set<DatoIntervallEntitet> fulleMåneder) {
        if (fulleMåneder.stream().anyMatch(periode -> periode.getTomDato().getMonth().equals(Month.APRIL) || periode.getTomDato().getMonth().equals(Month.MAY))) {
            Set<DatoIntervallEntitet> perioder = fulleMåneder.stream().filter(periode -> !(periode.getTomDato().getMonth().equals(Month.APRIL) || periode.getTomDato().getMonth().equals(Month.MAY))).collect(Collectors.toSet());
            // Håndterer april og mai som en periode (i samsvar med logikk i IkkeKantIKantVurderer)
            perioder.add(DatoIntervallEntitet.fraOgMedTilOgMed(APRIL_VILKÅRSPERIODE_FOM, MAI_VILKÅRSPERIODE_TOM));
            return perioder;
        }
        return fulleMåneder;
    }

    private DatoIntervallEntitet mapTilFullSøknadsmåned(Periode periode) {
        LocalDate fomDato = periode.getFom();
        if (fomDato.getMonth().equals(Month.APRIL) || fomDato.getMonth().equals(Month.MARCH)) {
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
