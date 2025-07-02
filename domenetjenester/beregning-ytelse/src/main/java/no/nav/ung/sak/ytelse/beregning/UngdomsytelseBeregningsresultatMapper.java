package no.nav.ung.sak.ytelse.beregning;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.kontrakt.beregningsresultat.BeregningsresultatDto;
import no.nav.ung.sak.kontrakt.beregningsresultat.BeregningsresultatMedUtbetaltePeriodeDto;
import no.nav.ung.sak.kontrakt.beregningsresultat.BeregningsresultatPeriodeDto;
import no.nav.ung.sak.ytelse.DagsatsOgUtbetalingsgrad;

@ApplicationScoped
public class UngdomsytelseBeregningsresultatMapper implements BeregningsresultatMapper {


    @Override
    public BeregningsresultatDto map(Behandling behandling,
                                     LocalDateTimeline<DagsatsOgUtbetalingsgrad> tilkjentYtelsePerioder) {
        return BeregningsresultatDto.build()
            .medPerioder(lagPerioder(tilkjentYtelsePerioder))
            .create();
    }

    @Override
    public BeregningsresultatMedUtbetaltePeriodeDto mapMedUtbetaltePerioder(Behandling behandling, LocalDateTimeline<DagsatsOgUtbetalingsgrad> tilkjentYtelsePerioder) {

        var perioder = lagPerioder(tilkjentYtelsePerioder);

        return BeregningsresultatMedUtbetaltePeriodeDto.build()
            .medPerioder(perioder)
            .medUtbetaltePerioder(perioder)
            .create();
    }

    public List<BeregningsresultatPeriodeDto> lagPerioder(LocalDateTimeline<DagsatsOgUtbetalingsgrad> tilkjentYtelsePerioder) {
        return tilkjentYtelsePerioder.stream().map(p ->
            BeregningsresultatPeriodeDto.build(p.getFom(), p.getTom())
                .medDagsats(p.getValue().dagsats().intValue())
                .create()).toList();

    }

}
