package no.nav.ung.sak.kontrakt.ungdomsytelse.beregning;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;
import no.nav.ung.kodeverk.ungdomsytelse.sats.UngdomsytelseSatsType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public record UngdomsytelseSatsPeriodeDto(
    @JsonProperty(value = "fom", required = true) @NotNull LocalDate fom,
    @JsonProperty(value = "tom", required = true) @NotNull LocalDate tom,
    @JsonProperty(value = "dagsats", required = true) @NotNull BigDecimal dagsats,
    @JsonProperty(value = "grunnbeløpFaktor", required = true) @NotNull BigDecimal grunnbeløpFaktor,
    @JsonProperty(value = "grunnbeløp", required = true) @NotNull BigDecimal grunnbeløp,
    @JsonProperty(value = "satsType", required = true) @NotNull UngdomsytelseSatsType satsType,
    @JsonProperty(value = "antallBarn", required = true) int antallBarn,
    @JsonProperty(value = "dagsatsBarnetillegg", required = true) @NotNull BigDecimal dagsatsBarnetillegg

) {
}
