package no.nav.ung.sak.perioder;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;

@ApplicationScoped
public class SøknadsfristTjenesteProvider {
    private Instance<VurderSøknadsfristTjeneste<?>> søknadsfristTjenester;

    public SøknadsfristTjenesteProvider() {
    }

    @Inject
    public SøknadsfristTjenesteProvider(@Any Instance<VurderSøknadsfristTjeneste<?>> søknadsfristTjenester) {
        this.søknadsfristTjenester = søknadsfristTjenester;
    }

    @SuppressWarnings("unchecked")
    public VurderSøknadsfristTjeneste<VurdertSøktPeriode.SøktPeriodeData> finnVurderSøknadsfristTjeneste(BehandlingReferanse ref) {
        FagsakYtelseType ytelseType = ref.getFagsakYtelseType();
        return (VurderSøknadsfristTjeneste<VurdertSøktPeriode.SøktPeriodeData>) FagsakYtelseTypeRef.Lookup.find(søknadsfristTjenester, ytelseType)
            .orElseThrow(() -> new UnsupportedOperationException("Har ikke " + VurderSøknadsfristTjeneste.class.getSimpleName() + " for ytelseType=" + ytelseType));
    }
}
