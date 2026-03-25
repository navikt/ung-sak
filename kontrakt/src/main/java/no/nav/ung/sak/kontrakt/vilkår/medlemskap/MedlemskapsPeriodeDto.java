package no.nav.ung.sak.kontrakt.vilkår.medlemskap;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.kontrakt.aktivitetspenger.medlemskap.MedlemskapAvslagsÅrsakType;
import no.nav.ung.sak.typer.Periode;

public record MedlemskapsPeriodeDto(
    @JsonProperty(value = "periode")
    Periode periode,
    @JsonProperty(value = "land")
    String land,
    @JsonProperty(value = "harTrygdeavtale")
    Boolean harTrygdeavtale,
    @JsonProperty(value = "vilkårUtfall")
    Utfall vilkårUtfall,
    @JsonProperty(value = "avslagsårsak")
    MedlemskapAvslagsÅrsakType avslagsårsak
) {
}
