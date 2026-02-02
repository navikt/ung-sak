package no.nav.ung.sak.kontrakt.oppgaver.typer.kontrollerregisterinntekt;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * DTO for registerinntekt med totalsummer.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    creatorVisibility = JsonAutoDetect.Visibility.NONE
)
public record RegisterinntektDTO(
    @JsonProperty(value = "arbeidOgFrilansInntekter", required = true)
    @NotNull
    List<ArbeidOgFrilansRegisterInntektDTO> arbeidOgFrilansInntekter,

    @JsonProperty(value = "ytelseInntekter", required = true)
    @NotNull
    List<YtelseRegisterInntektDTO> ytelseInntekter,

    @JsonProperty(value = "totalInntektArbeidOgFrilans", required = true)
    @NotNull
    Integer totalInntektArbeidOgFrilans,

    @JsonProperty(value = "totalInntektYtelse", required = true)
    @NotNull
    Integer totalInntektYtelse,

    @JsonProperty(value = "totalInntekt", required = true)
    @NotNull
    Integer totalInntekt
) {
    /**
     * Constructor som beregner totalsummer automatisk.
     */
    public RegisterinntektDTO(
        List<ArbeidOgFrilansRegisterInntektDTO> arbeidOgFrilansInntekter,
        List<YtelseRegisterInntektDTO> ytelseInntekter
    ) {
        this(
            arbeidOgFrilansInntekter,
            ytelseInntekter,
            arbeidOgFrilansInntekter.stream().mapToInt(ArbeidOgFrilansRegisterInntektDTO::inntekt).sum(),
            ytelseInntekter.stream().mapToInt(YtelseRegisterInntektDTO::inntekt).sum(),
            arbeidOgFrilansInntekter.stream().mapToInt(ArbeidOgFrilansRegisterInntektDTO::inntekt).sum() +
                ytelseInntekter.stream().mapToInt(YtelseRegisterInntektDTO::inntekt).sum()
        );
    }
}

