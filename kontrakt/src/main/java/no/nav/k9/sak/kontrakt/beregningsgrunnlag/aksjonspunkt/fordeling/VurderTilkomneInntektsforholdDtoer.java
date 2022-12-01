package no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.fordeling;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.gui.VurderNyttInntektsforholdDto;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_NYTT_INNTEKTSFORHOLD_KODE)
public class VurderTilkomneInntektsforholdDtoer extends BekreftetAksjonspunktDto {

    @JsonProperty(value = "grunnlag")
    @Valid
    @NotNull
    @Size(min = 1)
    private List<VurderTilkomneInntektsforholdDto> grunnlag;

    public VurderTilkomneInntektsforholdDtoer() {
        //
    }

    public VurderTilkomneInntektsforholdDtoer(List<VurderTilkomneInntektsforholdDto> grunnlag, String begrunnelse) { // NOSONAR
        super(begrunnelse);
        this.grunnlag = grunnlag;
    }

    public List<VurderTilkomneInntektsforholdDto> getGrunnlag() {
        return grunnlag;
    }
}
