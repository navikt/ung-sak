package no.nav.ung.sak.kontrakt.vilkår.medlemskap;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.ung.sak.typer.Periode;

import java.util.List;

public record MedlemskapDto(
    @JsonProperty(required = true)
    Periode forutgåendePeriode,

    @JsonProperty(required = true)
    boolean harBoddINorge,

    Boolean harJobbetINorge,

    Boolean harJobbetUtenforNorge,

    @JsonProperty(required = true)
    String journalpostId,

    @JsonProperty(required = true)
    List<UtenlandsoppholdDto> utenlandsopphold
) {
}
