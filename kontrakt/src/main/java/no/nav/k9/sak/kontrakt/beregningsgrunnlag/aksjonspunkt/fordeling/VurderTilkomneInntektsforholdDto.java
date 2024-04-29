package no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.fordeling;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.BekreftetBeregningsgrunnlagDto;
import no.nav.k9.sak.typer.Periode;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class VurderTilkomneInntektsforholdDto extends  BekreftetBeregningsgrunnlagDto {


    @JsonProperty("tilkomneInntektsforhold")
    @Valid
    @Size(min = 1, max = 500)
    private List<VurderTilkomneInntektsforholdPeriodeDto> tilkomneInntektsforholdPerioder;

    public VurderTilkomneInntektsforholdDto() {
        //
    }

    public VurderTilkomneInntektsforholdDto(@Valid @NotNull Periode periode, @Valid @Size(max = 500) List<VurderTilkomneInntektsforholdPeriodeDto> tilkomneInntektsforholdPerioder) {
        super(periode);
        this.tilkomneInntektsforholdPerioder = tilkomneInntektsforholdPerioder;
    }

    public List<VurderTilkomneInntektsforholdPeriodeDto> getTilkomneInntektsforholdPerioder() {
        return tilkomneInntektsforholdPerioder;
    }


}
