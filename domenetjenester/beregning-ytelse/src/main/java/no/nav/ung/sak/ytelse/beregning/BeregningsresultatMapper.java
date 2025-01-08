package no.nav.ung.sak.ytelse.beregning;

import java.util.List;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.kontrakt.beregningsresultat.BeregningsresultatDto;
import no.nav.ung.sak.kontrakt.beregningsresultat.BeregningsresultatMedUtbetaltePeriodeDto;
import no.nav.ung.sak.ytelse.DagsatsOgUtbetalingsgrad;

public interface BeregningsresultatMapper {

    BeregningsresultatDto map(Behandling behandling, LocalDateTimeline<DagsatsOgUtbetalingsgrad> tilkjentYtelsePerioder);

    BeregningsresultatMedUtbetaltePeriodeDto mapMedUtbetaltePerioder(Behandling behandling, LocalDateTimeline<DagsatsOgUtbetalingsgrad> tilkjentYtelsePerioder);
}
