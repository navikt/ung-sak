package no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.fordeling;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class VurderTilkomneInntektsforholdPeriodeDto {


    @JsonProperty("tilkomneInntektsforhold")
    @Valid
    @Size(min = 1, max = 100)
    private List<NyttInntektsforholdDto> tilkomneInntektsforhold;

    @JsonProperty("fom")
    @Valid
    @NotNull
    private LocalDate fom;

    @JsonProperty("tom")
    @Valid
    @NotNull
    private LocalDate tom;

    public VurderTilkomneInntektsforholdPeriodeDto() {
        //
    }

    public VurderTilkomneInntektsforholdPeriodeDto(List<NyttInntektsforholdDto> tilkomneInntektsforhold, LocalDate fom, LocalDate tom) {
        this.tilkomneInntektsforhold = tilkomneInntektsforhold;
        this.fom = fom;
        this.tom = tom;
    }

    public List<NyttInntektsforholdDto> getTilkomneInntektsforhold() {
        return tilkomneInntektsforhold;
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }
}
