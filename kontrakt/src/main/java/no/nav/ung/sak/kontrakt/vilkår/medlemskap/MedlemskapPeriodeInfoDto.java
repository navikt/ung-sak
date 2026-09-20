package no.nav.ung.sak.kontrakt.vilkår.medlemskap;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.kontrakt.aktivitetspenger.medlemskap.MedlemskapAvslagsÅrsakType;
import no.nav.ung.sak.typer.Periode;

public record MedlemskapPeriodeInfoDto(
    @JsonProperty(value = "periode", required = true)
    @NotNull
    @Valid
    Periode periode,

    @JsonProperty(value = "utfall", required = true)
    @NotNull
    @Valid
    Utfall utfall,

    @Valid
    MedlemskapAvslagsÅrsakType avslagsårsak,

    String begrunnelse,

    @JsonProperty(value = "vurderesIBehandlingen", required = true)
    boolean vurderesIBehandlingen,

    @JsonProperty(value = "erManueltVurdert", required = true)
    boolean erManueltVurdert,

    @Valid
    MedlemskapDto medlemskapFraBruker
) {
}
