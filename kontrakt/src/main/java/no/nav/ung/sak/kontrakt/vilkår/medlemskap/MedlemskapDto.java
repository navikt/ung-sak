package no.nav.ung.sak.kontrakt.vilkår.medlemskap;

import no.nav.ung.sak.typer.Periode;

import java.util.List;

public record MedlemskapDto(
    Periode forutgåendePeriode,
    boolean harBoddINorge,
    Boolean harJobbetINorge,
    Boolean harJobbetUtenforNorge,
    String journalpostId,
    List<UtenlandsoppholdDto> utenlandsopphold
) {
}
