package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

@JsonTypeName(AksjonspunktKodeDefinisjon.VURDERE_ANNEN_YTELSE_FØR_VEDTAK_KODE)
public class VurdereAnnenYteleseFørVedtakDto extends BekreftetAksjonspunktDto {


    VurdereAnnenYteleseFørVedtakDto() {
        // For Jackson
    }

    public VurdereAnnenYteleseFørVedtakDto(String begrunnelse) { // NOSONAR
        super(begrunnelse);
    }
}


