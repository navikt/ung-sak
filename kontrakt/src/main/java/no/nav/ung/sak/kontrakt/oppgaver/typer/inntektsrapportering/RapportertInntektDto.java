package no.nav.ung.sak.kontrakt.oppgaver.typer.inntektsrapportering;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.sak.kontrakt.oppgaver.BekreftelseDTO;

import java.math.BigDecimal;
import java.time.LocalDate;

public class RapportertInntektDto extends BekreftelseDTO {

    @JsonProperty(value = "fraOgMed", required = true)
    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate fraOgMed;

    @JsonProperty(value = "tilOgMed", required = true)
    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate tilOgMed;

    @JsonProperty(value = "arbeidstakerOgFrilansInntekt", required = true)
    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    private BigDecimal arbeidstakerOgFrilansInntekt;

    public RapportertInntektDto(LocalDate fraOgMed, LocalDate tilOgMed, BigDecimal arbeidstakerOgFrilansInntekt) {
        this.fraOgMed = fraOgMed;
        this.tilOgMed = tilOgMed;
        this.arbeidstakerOgFrilansInntekt = arbeidstakerOgFrilansInntekt;
    }

    public LocalDate getFraOgMed() {
        return fraOgMed;
    }

    public LocalDate getTilOgMed() {
        return tilOgMed;
    }

    public BigDecimal getArbeidstakerOgFrilansInntekt() {
        return arbeidstakerOgFrilansInntekt;
    }
}
