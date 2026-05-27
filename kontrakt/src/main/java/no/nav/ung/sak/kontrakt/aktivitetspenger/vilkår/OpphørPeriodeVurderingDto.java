package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.sak.typer.Periode;

import java.time.LocalDate;

public record OpphørPeriodeVurderingDto(

    @JsonProperty(value = "periode", required = true)
    @Valid
    @NotNull
    Periode periode,

    @JsonProperty("opphørDato")
    LocalDate opphørDato,

    @JsonProperty(value = "opphørÅrsak", required = true)
    @NotNull
    Avslagsårsak opphørÅrsak
) {
}
