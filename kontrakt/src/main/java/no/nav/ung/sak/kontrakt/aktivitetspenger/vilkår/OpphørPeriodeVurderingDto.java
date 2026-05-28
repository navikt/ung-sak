package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.sak.kontrakt.Patterns;
import no.nav.ung.sak.typer.Periode;

import java.time.LocalDate;

public record OpphørPeriodeVurderingDto(

    @JsonProperty(value = "periode", required = true)
    @Valid
    @NotNull
    Periode periode,

    @JsonProperty(value = "erOpphør", required = true)
    boolean erOpphør,

    @JsonProperty("opphørDato")
    LocalDate opphørDato,

    @JsonProperty("opphørÅrsak")
    Avslagsårsak opphørÅrsak,

    @JsonProperty("begrunnelse")
    @Size(max = 5000)
    @Pattern(regexp = Patterns.FRITEKST, message = Patterns.FRITEKST_MISMATCH_MELDING)
    String begrunnelse,

    @JsonProperty("fritekstVurderingBrev")
    @Size(max = 10000)
    @Pattern(regexp = Patterns.FRITEKST, message = Patterns.FRITEKST_MISMATCH_MELDING)
    String fritekstVurderingBrev
) {
}
