package no.nav.ung.sak.web.server.abac;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.interceptor.Interceptor;
import no.nav.k9.felles.log.sporingslogg.Sporingsdata;
import no.nav.k9.felles.sikkerhet.abac.DefaultAbacSporingslogg;
import no.nav.k9.felles.sikkerhet.abac.PdpRequest;
import no.nav.ung.sak.sikkerhet.abac.AppAbacAttributtType;

/**
 * Egen sporingslogg implementasjon for å utvide med egne felter.
 */
@ApplicationScoped
@Alternative
@Priority(Interceptor.Priority.APPLICATION + 1)
public class AppAbacSporingslogg extends DefaultAbacSporingslogg {

    /**
     * Eks. antall akjonspunkter, mottate dokumenter, el. som behandles i denne requesten.
     */
    @Override
    protected int getAntallResources(PdpRequest pdpRequest) {
        // en request kan i prinsippet inneholde mer enn ett aksjonspunkt (selv om uvanlig).
        return Math.max(1, pdpRequest.getAntall(AbacAttributter.RESOURCE_AKSJONSPUNKT_TYPE));
    }

    @Override
    protected void setCustomSporingsdata(PdpRequest pdpRequest, int index, Sporingsdata sporingsdata) {

        int antallIdenter = Math.max(1, antallIdenter(pdpRequest));
        setOptionalListValueinAttributeSet(sporingsdata, pdpRequest,
            AbacAttributter.RESOURCE_AKSJONSPUNKT_TYPE,
            (index / antallIdenter),
            AppAbacAttributtType.ABAC_AKSJONSPUNKT_TYPE);

        setOptionalValueinAttributeSet(sporingsdata, pdpRequest,
            AbacAttributter.RESOURCE_ANSVARLIG_SAKSBEHANDLER,
            AppAbacAttributtType.ABAC_ANSVALIG_SAKSBEHANDLER);

        setOptionalValueinAttributeSet(sporingsdata, pdpRequest,
            AbacAttributter.RESOURCE_BEHANDLINGSSTATUS,
            AppAbacAttributtType.ABAC_BEHANDLING_STATUS);

        setOptionalValueinAttributeSet(sporingsdata, pdpRequest,
            AbacAttributter.RESOURCE_SAKSSTATUS,
            AppAbacAttributtType.ABAC_SAK_STATUS);
    }

}
