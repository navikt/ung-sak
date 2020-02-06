package no.nav.k9.sak.kontrakt.vedtak;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.VURDERE_ANNEN_YTELSE_FØR_VEDTAK_KODE)
public class VurdereAnnenYteleseFørVedtakDto extends BekreftetAksjonspunktDto {

    protected VurdereAnnenYteleseFørVedtakDto() {
        //
    }

    public VurdereAnnenYteleseFørVedtakDto(String begrunnelse) { // NOSONAR
        super(begrunnelse);
    }
}


