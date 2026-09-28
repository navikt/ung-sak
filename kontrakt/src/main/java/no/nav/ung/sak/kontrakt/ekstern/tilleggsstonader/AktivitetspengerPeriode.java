package no.nav.ung.sak.kontrakt.ekstern.tilleggsstonader;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AktivitetspengerPeriode(
    @JsonProperty(value = "fom", required = true)
    @NotNull
    LocalDate fom,

    @JsonProperty(value = "tom", required = true)
    @NotNull
    LocalDate tom
) {
}
