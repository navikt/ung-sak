package no.nav.ung.sak.kontrakt.vedtak;

import java.util.Set;

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

    public BekreftVedtakUtenTotrinnskontrollDto() {
        //
    }

    public BekreftVedtakUtenTotrinnskontrollDto(String begrunnelse,
                                                String overskrift,
                                                String fritekstBrev,
                                                boolean skalBrukeOverstyrendeFritekstBrev,
                                                Set<String> redusertUtbetalingÅrsaker,
                                                boolean skalUndertrykke) { // NOSONAR
        super(begrunnelse, overskrift, fritekstBrev, skalBrukeOverstyrendeFritekstBrev, redusertUtbetalingÅrsaker, skalUndertrykke);
    }

}
