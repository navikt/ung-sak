package no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.søknadsfrist;

import no.nav.k9.sak.perioder.Søknad;
import no.nav.k9.sak.perioder.SøktPeriode;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;

import java.util.Set;

public interface SøknadsfristPeriodeVurderer {

    Set<VurdertSøktPeriode> vurderPeriode(Søknad søknadsDokument, Set<SøktPeriode> søktPeriode);
}
