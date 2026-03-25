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
    @JsonProperty(value = "sum", required = true)
    BigDecimal sum,

    @NotNull
    @Digits(integer = 6, fraction = 0)
    @JsonProperty(value = "sumAvkortet", required = true)
    BigDecimal sumAvkortet,

    @NotNull
    @Digits(integer = 6, fraction = 0)
    @JsonProperty(value = "sumAvkortetOgOppjustert", required = true)
    BigDecimal sumAvkortetOgOppjustert,

    @NotNull
    @Digits(integer = 6, fraction = 0)
    @JsonProperty(value = "arbeidsinntekt", required = true)
    BigDecimal arbeidsinntekt,

    @NotNull
    @Digits(integer = 6, fraction = 0)
    @JsonProperty(value = "næring", required = true)
    BigDecimal næring
) {
}
