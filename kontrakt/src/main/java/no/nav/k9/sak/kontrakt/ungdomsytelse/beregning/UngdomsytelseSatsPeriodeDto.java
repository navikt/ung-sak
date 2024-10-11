package no.nav.k9.sak.kontrakt.ungdomsytelse.beregning;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public record UngdomsytelseSatsPeriodeDto(
    @JsonProperty(value = "fom", required = true) LocalDate fom,
    @JsonProperty(value = "tom", required = true) LocalDate tom,
    @JsonProperty(value = "dagsats", required = true) BigDecimal dagsats,
    @JsonProperty(value = "grunnbeløpFaktor", required = true) BigDecimal grunnbeløpFaktor,
    @JsonProperty(value = "grunnbeløp", required = true) BigDecimal grunnbeløp
) {
}