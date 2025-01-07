package no.nav.ung.sak.ytelse.beregning;

import java.util.List;

import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.kontrakt.beregningsresultat.BeregningsresultatDto;
import no.nav.ung.sak.kontrakt.beregningsresultat.BeregningsresultatMedUtbetaltePeriodeDto;

public interface BeregningsresultatMapper {

    BeregningsresultatDto map(Behandling behandling, List<TilkjentYtelsePeriode> tilkjentYtelsePerioder);

    BeregningsresultatMedUtbetaltePeriodeDto mapMedUtbetaltePerioder(Behandling behandling, List<TilkjentYtelsePeriode> tilkjentYtelsePerioder);
}
