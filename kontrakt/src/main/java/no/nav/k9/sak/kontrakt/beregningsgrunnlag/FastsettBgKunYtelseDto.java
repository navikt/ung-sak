package no.nav.k9.sak.kontrakt.beregningsgrunnlag;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class FastsettBgKunYtelseDto {

    @JsonProperty(value = "andeler", required = true)
    @Valid
    @Size(min = 1, max = 100)
    private List<FastsattBrukersAndel> andeler;

    @JsonProperty(value = "skalBrukeBesteberegning")
    private Boolean skalBrukeBesteberegning;

    protected FastsettBgKunYtelseDto() {
        // For Jackson
    }

    public FastsettBgKunYtelseDto(List<FastsattBrukersAndel> andeler, Boolean skalBrukeBesteberegning) { // NOSONAR
        this.andeler = new ArrayList<>(andeler);
        this.skalBrukeBesteberegning = skalBrukeBesteberegning;
    }

    public List<FastsattBrukersAndel> getAndeler() {
        return andeler;
    }

    public void setAndeler(List<FastsattBrukersAndel> andeler) {
        this.andeler = andeler;
    }

    public Boolean getSkalBrukeBesteberegning() {
        return skalBrukeBesteberegning;
    }

    public void setSkalBrukeBesteberegning(Boolean skalBrukeBesteberegning) {
        this.skalBrukeBesteberegning = skalBrukeBesteberegning;
    }
}
