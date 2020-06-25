package no.nav.k9.sak.ytelse.frisinn.vilkår;

import java.time.LocalDate;
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
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitetPeriode;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.perioder.VilkårsPeriodiseringsFunksjon;
import no.nav.k9.sak.ytelse.frisinn.mapper.FrisinnMapper;

class BeregningPeriode implements VilkårsPeriodiseringsFunksjon {

    private final LocalDate skjæringstidspunkt = LocalDate.of(2020, 3, 1);
    private BehandlingRepository behandlingRepository;
    private UttakRepository uttakRepository;

    BeregningPeriode(BehandlingRepository behandlingRepository, UttakRepository uttakRepository) {
        this.behandlingRepository = behandlingRepository;
        this.uttakRepository = uttakRepository;
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> utledPeriode(Long behandlingId) {
        var uttakPerioder = uttakRepository.hentFastsattUttak(behandlingId).getPerioder();
        var origUttakPerioder = behandlingRepository.hentBehandling(behandlingId).getOriginalBehandling()
            .map(orig -> uttakRepository.hentFastsattUttak(orig.getId()).getPerioder())
            .orElse(Set.of());
        var søknadperioder = sammenslåPåMåneder(behandlingId, uttakPerioder);
        var origSøknadsperioder = sammenslåPåMåneder(behandlingId, origUttakPerioder);

        var nySøknadsperiode = finnNySøknadsperiode(origSøknadsperioder, søknadperioder);

        if (nySøknadsperiode.isPresent()) {
            return Collections.unmodifiableNavigableSet(new TreeSet<>(Set.of(nySøknadsperiode.get())));
        } else {
            return Collections.unmodifiableNavigableSet(søknadperioder.stream()
                .collect(Collectors.toCollection(TreeSet::new)));
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
                    .sorted(Comparator.reverseOrder())
                    .findFirst()
                    .orElseThrow();
                return DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
            })
            .collect(Collectors.toList());
        return sammenslåttePerioder;
    }

    private DatoIntervallEntitet hentMåned(DatoIntervallEntitet søknadsperiode, List<DatoIntervallEntitet> måneder) {
        return måneder.stream()
            .filter(måned -> søknadsperiode.overlapper(måned))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Søknadsperiode må være innenfor måneder"));
    }

    private Optional<DatoIntervallEntitet> finnNySøknadsperiode(List<DatoIntervallEntitet> perioderOrig, List<DatoIntervallEntitet> perioder) {
        if (perioder.size() > perioderOrig.size()) {
            return perioder.stream()
                .sorted(Comparator.comparing(DatoIntervallEntitet::getFomDato, Comparator.reverseOrder()))
                .findFirst();
        }
        return Optional.empty();
    }
}
