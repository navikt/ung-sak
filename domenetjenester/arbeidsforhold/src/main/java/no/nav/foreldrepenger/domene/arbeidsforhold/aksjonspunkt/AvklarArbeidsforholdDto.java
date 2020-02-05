package no.nav.foreldrepenger.domene.arbeidsforhold.aksjonspunkt;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_ARBEIDSFORHOLD_KODE)
public class AvklarArbeidsforholdDto extends BekreftetAksjonspunktDto {


    @Valid
    @Size(max = 1000)
    private List<ArbeidsforholdDto> arbeidsforhold;

    @SuppressWarnings("unused") // NOSONAR
    private AvklarArbeidsforholdDto() {
        super();
        //For Jackson
    }

    AvklarArbeidsforholdDto(String begrunnelse, List<ArbeidsforholdDto> arbeidsforhold) {
        super(begrunnelse);
        this.arbeidsforhold = arbeidsforhold;
    }

    public List<ArbeidsforholdDto> getArbeidsforhold() {
        return arbeidsforhold;
    }


}
