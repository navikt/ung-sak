package no.nav.ung.sak.kontrakt.vilkår.medlemskap;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.ung.sak.typer.Periode;

import java.time.LocalDateTime;

public record MedlemskapsPeriodeDto(
    @JsonProperty(value = "periode")
    Periode periode,
    @JsonProperty(value = "land")
    String land,
    @JsonProperty(value = "landkode")
    String landkode,
    @JsonProperty(value = "harTrygdeavtale")
    Boolean harTrygdeavtale,
    @JsonProperty(value = "journalpostId")
    String journalpostId,
    @JsonProperty(value = "mottattTidspunkt")
    LocalDateTime mottattTidspunkt
) {
}
