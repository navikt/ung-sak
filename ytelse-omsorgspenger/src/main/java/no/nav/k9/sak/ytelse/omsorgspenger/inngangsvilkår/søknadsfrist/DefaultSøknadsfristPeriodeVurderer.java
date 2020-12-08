package no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.søknadsfrist;

import no.nav.k9.sak.perioder.Søknad;
import no.nav.k9.sak.perioder.SøktPeriode;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;

import java.time.Period;
import java.util.Set;

public class DefaultSøknadsfristPeriodeVurderer implements SøknadsfristPeriodeVurderer {

    private final Period frist = Period.ofMonths(3);

    @Override
    public Set<VurdertSøktPeriode> vurderPeriode(Søknad søknadsDokument, Set<SøktPeriode> søktPeriode) {
        return null;
    }
}
