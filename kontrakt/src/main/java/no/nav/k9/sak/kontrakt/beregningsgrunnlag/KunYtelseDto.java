package no.nav.k9.sak.kontrakt.beregningsgrunnlag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class KunYtelseDto {

    @JsonProperty(value = "andeler", required = true)
    @NotNull
    @Valid
    @Size(max = 200)
    private List<AndelMedBeløpDto> andeler = new ArrayList<>();

    public List<AndelMedBeløpDto> getAndeler() {
        return Collections.unmodifiableList(andeler);
    }

    public void leggTilAndel(AndelMedBeløpDto andel) {
        andeler.add(andel);
    }

    public void setAndeler(List<AndelMedBeløpDto> andeler) {
        this.andeler = List.copyOf(andeler);
    }
}
