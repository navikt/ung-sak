package no.nav.k9.sak.ytelse.frisinn.vilkår;

import java.util.Set;

import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.inngangsvilkår.perioder.VilkårsPeriodiseringsFunksjon;

class MaksSøktePeriode implements VilkårsPeriodiseringsFunksjon {

    private UttakRepository uttakRepository;

    MaksSøktePeriode(UttakRepository uttakRepository) {
        this.uttakRepository = uttakRepository;
    }

    @Override
    public Set<DatoIntervallEntitet> utledPeriode(Long behandlingId) {
        var søknadsperioder = uttakRepository.hentOppgittSøknadsperioderHvisEksisterer(behandlingId);
        if (søknadsperioder.isEmpty()) {
            return Set.of();
        } else {
            return Set.of(søknadsperioder.get().getMaksPeriode());
        }
    }
}
