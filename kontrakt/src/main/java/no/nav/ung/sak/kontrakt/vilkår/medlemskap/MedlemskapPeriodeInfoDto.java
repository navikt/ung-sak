package no.nav.ung.sak.kontrakt.vilkår.medlemskap;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.kontrakt.aktivitetspenger.medlemskap.MedlemskapAvslagsÅrsakType;
import no.nav.ung.sak.typer.Periode;

public record MedlemskapPeriodeInfoDto(
    @JsonProperty(required = true)
    Periode periode,

    @JsonProperty(required = true)
    Utfall utfall,

    MedlemskapAvslagsÅrsakType avslagsårsak,

    String begrunnelse,

    @JsonProperty(required = true)
    boolean vurderesIBehandlingen,

    @JsonProperty(required = true)
    boolean erManueltVurdert,

    MedlemskapDto medlemskapFraBruker
) {
}
