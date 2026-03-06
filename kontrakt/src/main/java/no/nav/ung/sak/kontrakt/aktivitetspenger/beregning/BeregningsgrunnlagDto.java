package no.nav.ung.sak.kontrakt.aktivitetspenger.beregning;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public record BeregningsgrunnlagDto(
    @NotNull
    @JsonProperty("virkningstidspunkt")
    LocalDate virkningstidspunkt,

    @NotNull
    @Digits(integer = 6, fraction = 0)
    @JsonProperty("årsinntektSisteÅr")
    BigDecimal årsinntektSisteÅr,

    @NotNull
    @Digits(integer = 6, fraction = 0)
    @JsonProperty("årsinntektSisteTreÅr")
    BigDecimal årsinntektSisteTreÅr,

    @NotNull
    @Digits(integer = 6, fraction = 0)
    @JsonProperty("besteBeregning")
    BigDecimal besteBeregning,

    @NotNull
    @JsonProperty("pgiÅrsinntekter")
    List<PgiÅrsinntektDto> pgiÅrsinntekter
) {
}

