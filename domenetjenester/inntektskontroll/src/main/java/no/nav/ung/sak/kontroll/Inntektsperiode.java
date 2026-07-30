package no.nav.ung.sak.kontroll;

import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.Beløp;

public record Inntektsperiode(
    Beløp beløp,
    DatoIntervallEntitet periode
) {}
