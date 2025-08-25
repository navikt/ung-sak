package no.nav.ung.sak.web.server.abac;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.interceptor.Interceptor;
import no.nav.k9.felles.log.sporingslogg.Sporingsdata;
import no.nav.k9.felles.sikkerhet.abac.AbacAttributtType;
import no.nav.k9.felles.sikkerhet.abac.DefaultAbacSporingslogg;
import no.nav.k9.felles.sikkerhet.abac.PdpRequest;

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
            SporingsloggAttributtType.ABAC_AKSJONSPUNKT_TYPE);

        setOptionalValueinAttributeSet(sporingsdata, pdpRequest,
            AbacAttributter.RESOURCE_ANSVARLIG_SAKSBEHANDLER,
            SporingsloggAttributtType.ABAC_ANSVALIG_SAKSBEHANDLER);

        setOptionalValueinAttributeSet(sporingsdata, pdpRequest,
            AbacAttributter.RESOURCE_BEHANDLINGSSTATUS,
            SporingsloggAttributtType.ABAC_BEHANDLING_STATUS);

        setOptionalValueinAttributeSet(sporingsdata, pdpRequest,
            AbacAttributter.RESOURCE_SAKSSTATUS,
            SporingsloggAttributtType.ABAC_SAK_STATUS);
    }

    /**
     * attributt-typer som kun brukes i sporingslogg
     */
    private enum SporingsloggAttributtType implements AbacAttributtType {

        ABAC_ANSVALIG_SAKSBEHANDLER("ansvarlig_saksbehandler"),
        ABAC_BEHANDLING_STATUS("behandling_status"),
        ABAC_SAK_STATUS("sak_status"),
        ABAC_AKSJONSPUNKT_TYPE("aksjonspunkt_type"),
        ;

        private final boolean maskerOutput;
        private final String sporingsloggEksternKode;

        SporingsloggAttributtType(String sporingsloggEksternKode) {
            this.sporingsloggEksternKode = sporingsloggEksternKode;
            this.maskerOutput = false;
        }

        @Override
        public boolean getMaskerOutput() {
            return maskerOutput;
        }

        @Override
        public String getSporingsloggKode() {
            return sporingsloggEksternKode;
        }
    }

}
