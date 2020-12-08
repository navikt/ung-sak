package no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.søknadsfrist;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.Søknad;
import no.nav.k9.sak.perioder.SøktPeriode;
import no.nav.k9.sak.perioder.VurderSøknadsfristTjeneste;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;

import javax.enterprise.context.Dependent;
import java.util.Map;
import java.util.Set;

@Dependent
@FagsakYtelseTypeRef("OMP")
@BehandlingTypeRef
public class OMPVurderSøknadsfristTjeneste implements VurderSøknadsfristTjeneste {

    private Map<DatoIntervallEntitet, SøknadsfristPeriodeVurderer> avviksVurderere;
    private SøknadsfristPeriodeVurderer defaultVurderer = new DefaultSøknadsfristPeriodeVurderer();

    @Override
    public Map<Søknad, Set<VurdertSøktPeriode>> vurderSøknadsfrist(BehandlingReferanse behandlingReferanse,
                                                                   Map<Søknad, Set<SøktPeriode>> søknaderMedPerioder) {



        return null;
    }
}
