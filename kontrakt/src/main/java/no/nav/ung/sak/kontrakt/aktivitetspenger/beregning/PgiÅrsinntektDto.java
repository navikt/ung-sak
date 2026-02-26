package no.nav.ung.sak.kontrakt.aktivitetspenger.beregning;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public record PgiÅrsinntektDto(
    @NotNull @JsonProperty("årstall") int årstall,
    @NotNull @JsonProperty("pgi") BigDecimal pgi,
    @NotNull @JsonProperty("avkortetOgOppjustert") BigDecimal avkortetOgOppjustert
) {
}

