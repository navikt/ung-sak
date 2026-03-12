package no.nav.ung.ytelse.aktivitetspenger.beregning.beste;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.grunnbeløp.Grunnbeløp;
import no.nav.ung.sak.typer.Beløp;

import java.math.BigDecimal;

public record PgiKalkulatorInput(
    LocalDateTimeline<Beløp> årsinntekt,
    LocalDateTimeline<BigDecimal> oppjusteringsfaktorTidsserie,
    LocalDateTimeline<Grunnbeløp> gsnittTidsserie
) {}
