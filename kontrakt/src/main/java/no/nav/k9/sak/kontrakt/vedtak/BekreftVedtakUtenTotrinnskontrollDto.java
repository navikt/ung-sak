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
@JsonTypeName(AksjonspunktKodeDefinisjon.VEDTAK_UTEN_TOTRINNSKONTROLL_KODE)
public class BekreftVedtakUtenTotrinnskontrollDto extends VedtaksbrevOverstyringDto {

    protected BekreftVedtakUtenTotrinnskontrollDto() {
        //
    }

    public BekreftVedtakUtenTotrinnskontrollDto(String begrunnelse,
                                                String overskrift,
                                                String fritekstBrev,
                                                boolean skalBrukeOverstyrendeFritekstBrev) { // NOSONAR
        super(begrunnelse, overskrift, fritekstBrev, skalBrukeOverstyrendeFritekstBrev);
    }

}
