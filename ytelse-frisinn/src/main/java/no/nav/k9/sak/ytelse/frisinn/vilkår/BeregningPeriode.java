package no.nav.k9.sak.ytelse.frisinn.vilkår;

import java.time.LocalDate;
import java.util.Set;

import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.inngangsvilkår.perioder.VilkårsPeriodiseringsFunksjon;

class BeregningPeriode implements VilkårsPeriodiseringsFunksjon {

    private final LocalDate skjæringstidspunkt = LocalDate.of(2020, 3, 1);
    private UttakRepository uttakRepository;

    BeregningPeriode(UttakRepository uttakRepository) {
        this.uttakRepository = uttakRepository;
    }

    @Override
    public Set<DatoIntervallEntitet> utledPeriode(Long behandlingId) {
        var søknadsperioder = uttakRepository.hentOppgittSøknadsperioderHvisEksisterer(behandlingId);
        if (søknadsperioder.isEmpty()) {
            return Set.of();
        } else {
            var maksPeriode = søknadsperioder.get().getMaksPeriode();
            return Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt, maksPeriode.getTomDato()));
        }
    }
}
