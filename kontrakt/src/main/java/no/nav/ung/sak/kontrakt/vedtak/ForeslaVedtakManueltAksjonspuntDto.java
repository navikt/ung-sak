package no.nav.ung.sak.kontrakt.vedtak;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.FORESLÅ_VEDTAK_MANUELT_KODE)
public class ForeslaVedtakManueltAksjonspuntDto extends VedtaksbrevOverstyringDto {

    public ForeslaVedtakManueltAksjonspuntDto() {
        //
    }

    public ForeslaVedtakManueltAksjonspuntDto(String begrunnelse,
                                              String overskrift,
                                              String fritekstBrev,
                                              boolean skalBrukeOverstyrendeFritekstBrev,
                                              Set<String> redusertUtbetalingÅrsaker,
                                              boolean skalUndertrykke) {
        super(begrunnelse, overskrift, fritekstBrev, skalBrukeOverstyrendeFritekstBrev, redusertUtbetalingÅrsaker, skalUndertrykke);
    }

}
