package no.nav.k9.sak.kontrakt.uttak.overstyring;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class OverstyrbareUttakAktiviterDto {

    @JsonProperty(value = "arbeidsforholdsperioder")
    @Valid
    @NotNull
    @Size(max = 100)
    private List<OverstyrUttakArbeidsforholdDto> arbeidsforholdsperioder;

    public OverstyrbareUttakAktiviterDto() {
        //
    }

    public OverstyrbareUttakAktiviterDto(List<OverstyrUttakArbeidsforholdDto> arbeidsforholdsperioder) {
        this.arbeidsforholdsperioder = arbeidsforholdsperioder;
    }

    public List<OverstyrUttakArbeidsforholdDto> getArbeidsforholdsperioder() {
        return arbeidsforholdsperioder;
    }
}
