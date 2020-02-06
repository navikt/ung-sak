package no.nav.k9.sak.kontrakt.beregningsgrunnlag;

import java.time.LocalDate;
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
public class FastsattePerioderTidsbegrensetDto {

    @JsonProperty(value = "periodeFom", required = true)
    @NotNull
    private LocalDate periodeFom;

    @JsonProperty(value = "periodeTom")
    private LocalDate periodeTom;

    @JsonProperty(value = "fastsatteTidsbegrensedeAndeler")
    @Valid
    @Size(max = 200)
    private List<FastsatteAndelerTidsbegrensetDto> fastsatteTidsbegrensedeAndeler;

    protected FastsattePerioderTidsbegrensetDto() {
        // Jackson
    }

    public FastsattePerioderTidsbegrensetDto(LocalDate periodeFom,
                                             LocalDate periodeTom,
                                             List<FastsatteAndelerTidsbegrensetDto> fastsatteTidsbegrensedeAndeler) {
        this.periodeFom = periodeFom;
        this.periodeTom = periodeTom;
        this.fastsatteTidsbegrensedeAndeler = fastsatteTidsbegrensedeAndeler;
    }

    public LocalDate getPeriodeFom() {
        return periodeFom;
    }

    public LocalDate getPeriodeTom() {
        return periodeTom;
    }

    public List<FastsatteAndelerTidsbegrensetDto> getFastsatteTidsbegrensedeAndeler() {
        return fastsatteTidsbegrensedeAndeler;
    }
}
