package no.nav.ung.sak.mottak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;

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
