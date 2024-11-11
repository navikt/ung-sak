package no.nav.ung.sak.kontrakt.arbeidsforhold;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.ung.sak.kontrakt.Patterns;
import no.nav.ung.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_ARBEIDSFORHOLD_KODE)
public class AvklarArbeidsforhold extends BekreftetAksjonspunktDto {

    @JsonProperty(value = "arbeidsforhold")
    @Valid
    @Size(max = 1000)
    private List<AvklarArbeidsforholdDto> arbeidsforhold;

    public AvklarArbeidsforhold() {
        // For Jackson
    }

    @JsonCreator
    public AvklarArbeidsforhold(@JsonProperty("begrunnelse") @Size(max = 4000) @Pattern(regexp = Patterns.FRITEKST, message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]") String begrunnelse,
                                @JsonProperty(value = "arbeidsforhold", required = true) @Valid @Size(max = 1000) List<AvklarArbeidsforholdDto> arbeidsforhold) {
        super(begrunnelse);
        this.arbeidsforhold = arbeidsforhold;
    }

    public List<AvklarArbeidsforholdDto> getArbeidsforhold() {
        return arbeidsforhold;
    }

    public void setArbeidsforhold(List<AvklarArbeidsforholdDto> arbeidsforhold) {
        this.arbeidsforhold = arbeidsforhold;
    }

}
