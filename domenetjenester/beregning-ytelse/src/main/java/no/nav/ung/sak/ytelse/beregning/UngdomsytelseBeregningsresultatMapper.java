package no.nav.ung.sak.ytelse.beregning;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.ung.kodeverk.uttak.UtfallType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.beregningsresultat.BeregningsresultatDto;
import no.nav.ung.sak.kontrakt.beregningsresultat.BeregningsresultatMedUtbetaltePeriodeDto;
import no.nav.ung.sak.kontrakt.beregningsresultat.BeregningsresultatPeriodeAndelDto;
import no.nav.ung.sak.kontrakt.beregningsresultat.BeregningsresultatPeriodeDto;
import no.nav.ung.sak.kontrakt.beregningsresultat.UttakDto;
import no.nav.ung.sak.typer.Periode;

@ApplicationScoped
public class UngdomsytelseBeregningsresultatMapper implements BeregningsresultatMapper {


    @Override
    public BeregningsresultatDto map(Behandling behandling,
                                     List<TilkjentYtelsePeriode> tilkjentYtelsePerioder) {
        return BeregningsresultatDto.build()
            .medPerioder(lagPerioder(tilkjentYtelsePerioder))
            .create();
    }

    @Override
    public BeregningsresultatMedUtbetaltePeriodeDto mapMedUtbetaltePerioder(Behandling behandling, List<TilkjentYtelsePeriode> tilkjentYtelsePerioder) {

        var perioder = lagPerioder(tilkjentYtelsePerioder);

        return BeregningsresultatMedUtbetaltePeriodeDto.build()
            .medPerioder(perioder)
            .medUtbetaltePerioder(perioder)
            .create();
    }

    public List<BeregningsresultatPeriodeDto> lagPerioder(List<TilkjentYtelsePeriode> tilkjentYtelsePerioder) {
        var sisteUtbetalingsdato = finnSisteUtbetalingdato(tilkjentYtelsePerioder);
        return tilkjentYtelsePerioder.stream().map(p ->
            BeregningsresultatPeriodeDto.build(p.periode().getFomDato(), p.periode().getTomDato())
                .medDagsats(p.dagsats().intValue())
                .medAndeler(List.of(BeregningsresultatPeriodeAndelDto.build()
                    .medTilSøker(p.dagsats().intValue())
                    .medUtbetalingsgrad(p.utbetalingsgrad())
                    .medUttak(List.of(new UttakDto(new Periode(
                        p.periode().getFomDato(),
                        p.periode().getTomDato()),
                        p.utbetalingsgrad().compareTo(BigDecimal.ZERO) > 0 ? UtfallType.INNVILGET : UtfallType.AVSLÅTT, p.utbetalingsgrad())))
                    .medSisteUtbetalingsdato(sisteUtbetalingsdato.orElse(null))
                    .create()))
                .create()).toList();

    }

    private Optional<LocalDate> finnSisteUtbetalingdato(List<TilkjentYtelsePeriode> tilkjentYtelsePerioder) {
        return tilkjentYtelsePerioder.stream()
            .filter(p -> p.dagsats().compareTo(BigDecimal.ZERO) > 0)
            .map(TilkjentYtelsePeriode::periode)
            .map(DatoIntervallEntitet::getTomDato)
            .max(Comparator.naturalOrder());
    }

}
