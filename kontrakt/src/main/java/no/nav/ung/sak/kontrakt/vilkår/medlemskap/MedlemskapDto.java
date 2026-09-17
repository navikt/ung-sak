package no.nav.ung.sak.kontrakt.vilkår.medlemskap;

import java.util.List;

public record MedlemskapDto(
    boolean harBoddINorge,
    Boolean harJobbetINorge,
    Boolean harJobbetUtenforNorge,
    String journalpostId,
    List<UtenlandsoppholdDto> utenlandsopphold
) {
}
