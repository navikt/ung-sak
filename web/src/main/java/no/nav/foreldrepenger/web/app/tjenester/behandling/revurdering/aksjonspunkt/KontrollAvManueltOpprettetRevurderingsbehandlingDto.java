package no.nav.foreldrepenger.web.app.tjenester.behandling.revurdering.aksjonspunkt;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

@JsonTypeName(AksjonspunktKodeDefinisjon.KONTROLL_AV_MANUELT_OPPRETTET_REVURDERINGSBEHANDLING_KODE)
public class KontrollAvManueltOpprettetRevurderingsbehandlingDto  extends BekreftetAksjonspunktDto {

    public KontrollAvManueltOpprettetRevurderingsbehandlingDto() {
        //For Jackson
    }

}


