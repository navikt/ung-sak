package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

@JsonTypeName(AksjonspunktKodeDefinisjon.VURDERE_DOKUMENT_FØR_VEDTAK_KODE)
public class VurdereDokumentFørVedtakDto extends BekreftetAksjonspunktDto {


    VurdereDokumentFørVedtakDto() {
        // For Jackson
    }

    public VurdereDokumentFørVedtakDto(String begrunnelse) { // NOSONAR
        super(begrunnelse);
    }

}
