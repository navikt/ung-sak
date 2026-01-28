package no.nav.ung.sak.kontroll;

import no.nav.ung.sak.felles.tid.DatoIntervallEntitet;
import no.nav.ung.sak.felles.typer.Beløp;

public record Inntektsperiode(
    Beløp beløp,
    DatoIntervallEntitet periode
) {}
