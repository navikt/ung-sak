package no.nav.ung.sak.kontrakt.vilkår.medlemskap;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.sak.typer.Periode;

import java.util.List;

public record MedlemskapDto(
    @JsonProperty(required = true)
    @NotNull
    @Valid
    Periode forutgåendePeriode,

    @JsonProperty(required = true)
    boolean harBoddINorge,

    Boolean harJobbetINorge,

    Boolean harJobbetUtenforNorge,

    @JsonProperty(required = true)
    @NotNull
    String journalpostId,

    @JsonProperty(required = true)
    @NotNull
    List<UtenlandsoppholdDto> utenlandsopphold
) {
}
