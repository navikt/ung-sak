package no.nav.ung.sak.økonomi.tilkjentytelse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.oppdrag.kontrakt.kodeverk.Inntektskategori;
import no.nav.k9.oppdrag.kontrakt.kodeverk.SatsType;
import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.TilkjentYtelseAndelV1;
import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.TilkjentYtelsePeriodeV1;
import no.nav.ung.sak.ytelse.beregning.TilkjentYtelsePeriode;

public class MapperForTilkjentYtelse {

    private static final Logger logger = LoggerFactory.getLogger(MapperForTilkjentYtelse.class);

    public static List<TilkjentYtelsePeriodeV1> mapTilkjentYtelse(List<TilkjentYtelsePeriode> perioder) {
        if (perioder == null) {
            return Collections.emptyList();
        }
        return perioder
            .stream()
            .map(MapperForTilkjentYtelse::mapPeriode)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private static TilkjentYtelsePeriodeV1 mapPeriode(TilkjentYtelsePeriode periode) {
        if (periode.dagsats() == 0) {
            logger.info("Periode {}-{} hadde ingen beløp over 0 og ble ignorert", periode.periode().getFomDato(), periode.periode().getTomDato());
            return null;
        }
        return new TilkjentYtelsePeriodeV1(periode.periode().getFomDato(), periode.periode().getTomDato(), List.of(
            new TilkjentYtelseAndelV1(true, Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER, periode.dagsats(), SatsType.DAG, periode.utbetalingsgrad())));
    }

}
