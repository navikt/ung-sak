package no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
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
@JsonTypeName(AksjonspunktKodeDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_TIDSBEGRENSET_ARBEIDSFORHOLD_KODE)
public class FastsettBGTidsbegrensetArbeidsforholdDtoer extends BekreftetAksjonspunktDto {

    @JsonProperty(value = "grunnlag")
    @Valid
    @NotNull
    @Size(min = 1)
    private List<FastsettBGTidsbegrensetArbeidsforholdDto> grunnlag;

    public FastsettBGTidsbegrensetArbeidsforholdDtoer() {
        // For Jackson
    }

    public FastsettBGTidsbegrensetArbeidsforholdDtoer(String begrunnelse, List<FastsettBGTidsbegrensetArbeidsforholdDto> grunnlag) { // NOSONAR
        super(begrunnelse);
        this.grunnlag = new ArrayList<>(grunnlag);
    }

    public List<FastsettBGTidsbegrensetArbeidsforholdDto> getGrunnlag() {
        return grunnlag;
    }
}
