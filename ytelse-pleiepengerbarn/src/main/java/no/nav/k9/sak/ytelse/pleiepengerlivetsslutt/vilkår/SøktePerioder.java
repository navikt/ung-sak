package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.vilkår;

import java.util.NavigableSet;

import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPeriodiseringsFunksjon;
import no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.repo.søknadsperiode.SøknadsperiodeTjeneste;

class SøktePerioder implements VilkårsPeriodiseringsFunksjon {

    private SøknadsperiodeTjeneste søknadsperiodeTjeneste;


    SøktePerioder(SøknadsperiodeTjeneste søknadsperiodeTjeneste) {
        this.søknadsperiodeTjeneste = søknadsperiodeTjeneste;
    }


    @Override
    public NavigableSet<DatoIntervallEntitet> utledPeriode(Long behandlingId) {
        return søknadsperiodeTjeneste.utledPeriode(behandlingId);
    }
}
