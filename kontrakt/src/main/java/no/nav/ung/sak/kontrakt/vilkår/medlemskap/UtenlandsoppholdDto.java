package no.nav.ung.sak.kontrakt.vilkår.medlemskap;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.ung.sak.typer.Periode;

public record UtenlandsoppholdDto(
    @JsonProperty(required = true)
    Periode periode,

    @JsonProperty(required = true)
    String land,

    @JsonProperty(required = true)
    String landkode,

    @JsonProperty(required = true)
    boolean harTrygdeavtale,

    Boolean harJobbetIPerioden,

    String utenlandskNasjonalId
) {
}
