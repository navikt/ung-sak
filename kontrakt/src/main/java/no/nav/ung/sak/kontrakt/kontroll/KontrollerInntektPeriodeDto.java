package no.nav.ung.sak.kontrakt.kontroll;

import no.nav.ung.sak.typer.Periode;

public record KontrollerInntektPeriodeDto(
    Periode periode,
    PeriodeStatus status,
    boolean erTilVurdering,
    RapporterteInntekterDto rapporterteInntekter,
    Integer fastsattInntekt,
    BrukKontrollertInntektValg valg,
    String uttalelseFraBruker
) {
}
