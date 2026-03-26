package no.nav.ung.sak.kontrakt.vilkår.medlemskap;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.ung.sak.typer.Periode;

public record MedlemskapsPeriodeDto(
    @JsonProperty(value = "periode")
    Periode periode,
    @JsonProperty(value = "landkode")
    String landkode,
    @JsonProperty(value = "harTrygdeavtale")
    Boolean harTrygdeavtale
) {
}
