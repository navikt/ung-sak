package no.nav.foreldrepenger.inngangsvilkaar.perioder;

import java.util.Set;

import no.nav.foreldrepenger.behandlingslager.behandling.fordeling.FordelingRepository;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;

class MaksSøktePeriode implements VilkårsPeriodiseringsFunksjon {

    private FordelingRepository fordelingRepository;

    MaksSøktePeriode(FordelingRepository fordelingRepository) {
        this.fordelingRepository = fordelingRepository;
    }

    @Override
    public Set<DatoIntervallEntitet> utledPeriode(Long behandlingId) {
        var fordeling = fordelingRepository.hentHvisEksisterer(behandlingId);
        if (fordeling.isEmpty()) {
            return Set.of();
        } else {
            return Set.of(fordeling.get().getMaksPeriode());
        }
    }
}
