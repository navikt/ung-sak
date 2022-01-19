package no.nav.k9.sak.mottak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;

@ApplicationScoped
public class SøknadMottakTjenesteContainer {
    private Instance<SøknadMottakTjeneste<?>> søknadMottakere;

    protected SøknadMottakTjenesteContainer() {
        // for proxy
    }

    @Inject
    public SøknadMottakTjenesteContainer(@Any Instance<SøknadMottakTjeneste<?>> søknadMottakere) {
        this.søknadMottakere = søknadMottakere;
    }

    @SuppressWarnings("rawtypes")
    public SøknadMottakTjeneste finnSøknadMottakerTjeneste(FagsakYtelseType ytelseType) {
        return FagsakYtelseTypeRef.Lookup.find(søknadMottakere, ytelseType)
            .orElseThrow(() -> new UnsupportedOperationException("Har ikke støtte for ytelseType:" + ytelseType));
    }

}
