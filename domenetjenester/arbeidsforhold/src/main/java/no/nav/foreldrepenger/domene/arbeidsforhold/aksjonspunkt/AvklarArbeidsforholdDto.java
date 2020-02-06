package no.nav.foreldrepenger.domene.arbeidsforhold.aksjonspunkt;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_ARBEIDSFORHOLD_KODE)
public class AvklarArbeidsforholdDto extends BekreftetAksjonspunktDto {

    @JsonProperty(value="arbeidsforhold")
    @Valid
    @Size(max = 1000)
    private List<ArbeidsforholdDto> arbeidsforhold;

    AvklarArbeidsforholdDto() {
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
