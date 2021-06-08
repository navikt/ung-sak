package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett;

import java.util.Collections;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPeriodiseringsFunksjon;

public class UtvidetRettSøknadPerioder implements VilkårsPeriodiseringsFunksjon {

    private SøknadRepository søknadRepository;

    public UtvidetRettSøknadPerioder(SøknadRepository søknadRepository) {
        this.søknadRepository = søknadRepository;
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> utledPeriode(Long behandlingId) {
        var søknad = søknadRepository.hentSøknad(behandlingId);
        var søknadsperiode = søknad.getSøknadsperiode();
        return Collections.unmodifiableNavigableSet(new TreeSet<>(Set.of(søknadsperiode)));
    }
}
