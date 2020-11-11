package no.nav.k9.sak.ytelse.unntaksbehandling.vilkår;

import java.util.Collections;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPeriodiseringsFunksjon;

class FagsakPeriode implements VilkårsPeriodiseringsFunksjon {

    private BehandlingRepository behandlingRepository;

    FagsakPeriode(BehandlingRepository behandlingRepository) {
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> utledPeriode(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var fagsakPeriode = behandling.getFagsak().getPeriode();
        return Collections.unmodifiableNavigableSet(new TreeSet<>(Set.of(
            DatoIntervallEntitet.fraOgMedTilOgMed(fagsakPeriode.getFomDato(), fagsakPeriode.getTomDato()))
        ));

    }
}
