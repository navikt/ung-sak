package no.nav.ung.sak.kontrakt.kontroll;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.sak.typer.Periode;

public record FastsettInntektPeriodeDto(
    @JsonProperty(value = "periode", required = true)
    @NotNull
    @Valid
    Periode periode,

    @JsonProperty(value = "fastsattInnntekt")
    @Min(0)
    @Max(1000000)
    Integer fastsattInnntekt,

    @JsonProperty(value = "valg", required = true)
    @NotNull
    @Valid
    BrukKontrollertInntektValg valg,

    @JsonProperty(value = "begrunnelse", required = true)
    @NotNull
    @Valid
    String begrunnelse
) {

    @AssertTrue(message = "Må ha satt inntekt for valg MANUELT_FASTSATT")
    public boolean isHarInntektForManueltFastsatt() {
        return !valg.equals(BrukKontrollertInntektValg.MANUELT_FASTSATT) || fastsattInnntekt != null;
    }
}
