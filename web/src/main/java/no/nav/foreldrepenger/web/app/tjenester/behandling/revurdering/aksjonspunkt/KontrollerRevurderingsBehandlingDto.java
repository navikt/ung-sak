package no.nav.foreldrepenger.web.app.tjenester.behandling.revurdering.aksjonspunkt;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

@JsonTypeName(AksjonspunktKodeDefinisjon.KONTROLLER_REVURDERINGSBEHANDLING_VARSEL_VED_UGUNST_KODE)
public class KontrollerRevurderingsBehandlingDto extends BekreftetAksjonspunktDto {

    public KontrollerRevurderingsBehandlingDto() {
        //For Jackson
    }

}
