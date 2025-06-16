package no.nav.ung.sak.økonomi.tilkjentytelse;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.oppdrag.kontrakt.kodeverk.Inntektskategori;
import no.nav.k9.oppdrag.kontrakt.kodeverk.SatsType;
import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.TilkjentYtelseAndelV1;
import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.TilkjentYtelsePeriodeV1;
import no.nav.ung.sak.ytelse.DagsatsOgUtbetalingsgrad;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MapperForTilkjentYtelse {

    private static final Logger logger = LoggerFactory.getLogger(MapperForTilkjentYtelse.class);

    public static List<TilkjentYtelsePeriodeV1> mapTilkjentYtelse(LocalDateTimeline<DagsatsOgUtbetalingsgrad> perioder) {
        if (perioder == null) {
            return Collections.emptyList();
        }
        return perioder
            .stream()
            .map(MapperForTilkjentYtelse::mapPeriode)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private static TilkjentYtelsePeriodeV1 mapPeriode(LocalDateSegment<DagsatsOgUtbetalingsgrad> periode) {
        if (periode.getValue().dagsats() == 0) {
            logger.info("Periode {}-{} hadde ingen beløp over 0 og ble ignorert", periode.getFom(), periode.getTom());
            return null;
        }
        Inntektskategori inntektskategori = Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER;
        return new TilkjentYtelsePeriodeV1(periode.getFom(), periode.getTom(), List.of(
            new TilkjentYtelseAndelV1(true, inntektskategori, periode.getValue().dagsats(), SatsType.DAG, periode.getValue().utbetalingsgrad())));
    }

}
