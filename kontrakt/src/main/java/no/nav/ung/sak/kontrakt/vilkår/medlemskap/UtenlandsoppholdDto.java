package no.nav.ung.sak.kontrakt.vilkår.medlemskap;

import no.nav.ung.sak.typer.Periode;

public record UtenlandsoppholdDto(
    Periode periode,
    String land,
    String landkode,
    boolean harTrygdeavtale,
    Boolean harJobbetIPerioden,
    String utenlandskNasjonalId) {
}
