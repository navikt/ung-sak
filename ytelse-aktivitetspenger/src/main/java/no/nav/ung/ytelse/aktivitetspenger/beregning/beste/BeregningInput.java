package no.nav.ung.ytelse.aktivitetspenger.beregning.beste;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.grunnbeløp.Grunnbeløp;
import no.nav.ung.sak.typer.Beløp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;

public record BeregningInput(
    LocalDate virkningsdato,
    Year sistLignedeÅr,
    LocalDateTimeline<Beløp> årsinntektMap,
    LocalDateTimeline<BigDecimal> inflasjonsfaktorTidsserie,
    LocalDateTimeline<Grunnbeløp> gsnittTidsserie
) {}
