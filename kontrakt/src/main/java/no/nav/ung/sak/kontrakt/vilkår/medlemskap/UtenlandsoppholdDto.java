package no.nav.ung.sak.kontrakt.vilkår.medlemskap;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.sak.typer.Periode;

public record UtenlandsoppholdDto(
    @JsonProperty(required = true)
    @NotNull
    @Valid
    Periode periode,

    @JsonProperty(required = true)
    @NotNull
    String land,

    @JsonProperty(required = true)
    @NotNull
    String landkode,

    @JsonProperty(required = true)
    boolean harTrygdeavtale,

    Boolean harJobbetIPerioden,

    String utenlandskNasjonalId
) {
}
