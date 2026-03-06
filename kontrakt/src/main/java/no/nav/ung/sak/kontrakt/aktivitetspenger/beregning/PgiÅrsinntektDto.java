package no.nav.ung.sak.kontrakt.aktivitetspenger.beregning;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public record PgiÅrsinntektDto(

    @NotNull
    @JsonProperty(value = "årstall", required = true)
    int årstall,

    @NotNull
    @Digits(integer = 6, fraction = 0)
    @JsonProperty(value = "pgiÅrsinntekt", required = true)
    BigDecimal pgiÅrsinntekt,

    @NotNull
    @Digits(integer = 6, fraction = 0)
    @JsonProperty(value = "avkortetOgOppjustert", required = true)
    BigDecimal avkortetOgOppjustert
) {
}
