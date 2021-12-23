package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår;

import java.util.NavigableSet;

import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPeriodiseringsFunksjon;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeTjeneste;

public class SøktePerioder implements VilkårsPeriodiseringsFunksjon {

    private SøknadsperiodeTjeneste søknadsperiodeTjeneste;


    public SøktePerioder(SøknadsperiodeTjeneste søknadsperiodeTjeneste) {
        this.søknadsperiodeTjeneste = søknadsperiodeTjeneste;
    }


    @Override
    public NavigableSet<DatoIntervallEntitet> utledPeriode(Long behandlingId) {
        return søknadsperiodeTjeneste.utledPeriode(behandlingId);
    }
}
