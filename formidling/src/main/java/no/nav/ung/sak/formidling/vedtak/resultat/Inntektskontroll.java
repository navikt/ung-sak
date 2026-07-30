package no.nav.ung.sak.formidling.vedtak.resultat;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;

public final class Inntektskontroll {

    private Inntektskontroll() {
    }

    public static boolean erInntektReduksjon(DetaljertResultat r) {
        return erKontrollAvInntektMedTilkjentYtelse(r) && r.utbetalingsgrad().erRedusert();
    }

    public static boolean erInntektFullUtbetaling(DetaljertResultat r) {
        return erKontrollAvInntektMedTilkjentYtelse(r) && r.utbetalingsgrad() == UtbetalingsgradType.FULL;
    }

    public static boolean harInntektReduksjon(LocalDateTimeline<DetaljertResultat> resultatTidslinje) {
        return resultatTidslinje.stream().anyMatch(it -> erInntektReduksjon(it.getValue()));
    }

    public static boolean harInntektFullUtbetaling(LocalDateTimeline<DetaljertResultat> resultatTidslinje) {
        return resultatTidslinje.stream().anyMatch(it -> erInntektFullUtbetaling(it.getValue()));
    }

    public static LocalDateTimeline<DetaljertResultat> fullUtbetalingTidslinje(LocalDateTimeline<DetaljertResultat> resultatTidslinje) {
        return resultatTidslinje.filterValue(Inntektskontroll::erInntektFullUtbetaling);
    }

    /**
     * Tilkjent ytelse beregnes fra de kontrollerte inntektsperiodene, så manglende utbetalingsgrad betyr
     * at kontrollen ikke har gitt noe resultat for perioden.
     */
    private static boolean erKontrollAvInntektMedTilkjentYtelse(DetaljertResultat r) {
        return r.harÅrsak(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT)
            && r.utbetalingsgrad().erSatt();
    }

}
