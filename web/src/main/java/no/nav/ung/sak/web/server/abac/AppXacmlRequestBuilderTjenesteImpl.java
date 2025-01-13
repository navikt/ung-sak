package no.nav.ung.sak.web.server.abac;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Alternative;
import jakarta.interceptor.Interceptor;
import no.nav.k9.felles.sikkerhet.abac.PdpRequest;
import no.nav.k9.felles.sikkerhet.pdp.XacmlRequestBuilderTjeneste;
import no.nav.k9.felles.sikkerhet.pdp.xacml.XacmlRequestBuilder;

@Dependent
@Alternative
@Priority(Interceptor.Priority.APPLICATION + 2)
public class AppXacmlRequestBuilderTjenesteImpl implements XacmlRequestBuilderTjeneste {

    public AppXacmlRequestBuilderTjenesteImpl() {
    }

    @Override
    public XacmlRequestBuilder lagXacmlRequestBuilder(PdpRequest pdpRequest) {
        throw new IllegalArgumentException("Ikke i bruk");
    }

}
