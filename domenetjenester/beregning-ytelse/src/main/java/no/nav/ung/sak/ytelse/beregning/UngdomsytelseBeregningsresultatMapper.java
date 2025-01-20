package no.nav.ung.sak.ytelse.beregning;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.uttak.UtfallType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.kontrakt.beregningsresultat.BeregningsresultatDto;
import no.nav.ung.sak.kontrakt.beregningsresultat.BeregningsresultatMedUtbetaltePeriodeDto;
import no.nav.ung.sak.kontrakt.beregningsresultat.BeregningsresultatPeriodeAndelDto;
import no.nav.ung.sak.kontrakt.beregningsresultat.BeregningsresultatPeriodeDto;
import no.nav.ung.sak.kontrakt.beregningsresultat.UttakDto;
import no.nav.ung.sak.typer.Periode;
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
        var sisteUtbetalingsdato = finnSisteUtbetalingdato(tilkjentYtelsePerioder);
        return tilkjentYtelsePerioder.stream().map(p ->
            BeregningsresultatPeriodeDto.build(p.getFom(), p.getTom())
                .medDagsats(p.getValue().dagsats().intValue())
                .medAndeler(List.of(BeregningsresultatPeriodeAndelDto.build()
                    .medTilSøker(p.getValue().dagsats().intValue())
                    .medUtbetalingsgrad(p.getValue().utbetalingsgrad())
                    .medUttak(List.of(new UttakDto(new Periode(
                        p.getFom(),
                        p.getTom()),
                        p.getValue().utbetalingsgrad().compareTo(BigDecimal.ZERO) > 0 ? UtfallType.INNVILGET : UtfallType.AVSLÅTT, p.getValue().utbetalingsgrad())))
                    .medSisteUtbetalingsdato(sisteUtbetalingsdato.orElse(null))
                    .create()))
                .create()).toList();

    }

    private Optional<LocalDate> finnSisteUtbetalingdato(LocalDateTimeline<DagsatsOgUtbetalingsgrad> tilkjentYtelsePerioder) {
        var utbetalt = tilkjentYtelsePerioder
            .filterValue(p -> p.dagsats() > 0);
        return utbetalt.isEmpty() ? Optional.empty() : Optional.of(utbetalt.getMaxLocalDate());
    }

}
