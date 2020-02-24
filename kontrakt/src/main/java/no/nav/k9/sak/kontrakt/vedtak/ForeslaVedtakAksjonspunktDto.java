package no.nav.k9.sak.kontrakt.vedtak;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.FORESLÅ_VEDTAK_KODE)
public class ForeslaVedtakAksjonspunktDto extends VedtaksbrevOverstyringDto {

    public ForeslaVedtakAksjonspunktDto() {
        //
    }

    public ForeslaVedtakAksjonspunktDto(String begrunnelse,
                                        String overskrift,
                                        String fritekst,
                                        boolean skalBrukeOverstyrendeFritekstBrev) {
        super(begrunnelse, overskrift, fritekst, skalBrukeOverstyrendeFritekstBrev);
    }

}
