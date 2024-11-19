package no.nav.ung.sak.kontrakt.vedtak;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.ung.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.VURDERE_OVERLAPPENDE_YTELSER_FØR_VEDTAK_KODE)
public class VurdereOverlappendeYteleseFørVedtakDto extends BekreftetAksjonspunktDto {

    public VurdereOverlappendeYteleseFørVedtakDto() {
        //
    }

    public VurdereOverlappendeYteleseFørVedtakDto(String begrunnelse) { // NOSONAR
        super(begrunnelse);
    }
}
