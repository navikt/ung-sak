package no.nav.k9.sak.ytelse.frisinn.vilkår;

import java.util.Collections;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitetPeriode;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.perioder.VilkårsPeriodiseringsFunksjon;

class BeregningPeriode implements VilkårsPeriodiseringsFunksjon {

    private UttakRepository uttakRepository;

    BeregningPeriode(UttakRepository uttakRepository) {
        this.uttakRepository = uttakRepository;
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> utledPeriode(Long behandlingId) {
        // TODO (essv): Håndtere perioder fra både SN og FL i samme tidsrom
        var uttakAktivitet = uttakRepository.hentFastsattUttakHvisEksisterer(behandlingId);
        if (uttakAktivitet.isEmpty()) {
            return Collections.emptyNavigableSet();
        } else {
            return Collections.unmodifiableNavigableSet(
                uttakAktivitet.get().getPerioder().stream()
                    .map(UttakAktivitetPeriode::getPeriode)
                    .map(periode -> DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFomDato(), periode.getTomDato()))
                    .collect(Collectors.toCollection(TreeSet::new))
            );
        }
    }
}
