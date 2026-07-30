package no.nav.ung.sak.formidling.vedtak.resultat;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.KontrollertInntektPeriode;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;

import java.math.BigDecimal;

public final class EndringInntektUtleder {

    private EndringInntektUtleder() {
    }

    public static boolean erInntektReduksjon(DetaljertResultat r) {
        return erKontrollAvInntektMedTilkjentYtelse(r) && r.utbetalingsgrad().erRedusert();
    }

    public static boolean erInntektFullUtbetaling(DetaljertResultat r) {
        return erKontrollAvInntektMedTilkjentYtelse(r) && r.utbetalingsgrad() == Utbetalingsgrad.FULL;
    }

    public static boolean harInntektReduksjon(LocalDateTimeline<DetaljertResultat> resultatTidslinje) {
        return resultatTidslinje.stream().anyMatch(it -> erInntektReduksjon(it.getValue()));
    }

    public static boolean harInntektFullUtbetaling(LocalDateTimeline<DetaljertResultat> resultatTidslinje) {
        return resultatTidslinje.stream().anyMatch(it -> erInntektFullUtbetaling(it.getValue()));
    }

    public static LocalDateTimeline<DetaljertResultat> fullUtbetalingTidslinje(LocalDateTimeline<DetaljertResultat> resultatTidslinje) {
        return resultatTidslinje.filterValue(EndringInntektUtleder::erInntektFullUtbetaling);
    }

    public static boolean harManuellFastsatt0kr(KontrollertInntektPeriode kontrollertInntektPeriode) {
        boolean harFastsattInntektTil0kr = kontrollertInntektPeriode.getInntekt().compareTo(BigDecimal.ZERO) == 0;
        return kontrollertInntektPeriode.getErManueltVurdert() && harFastsattInntektTil0kr;
    }

    public static LocalDateTimeline<KontrollertInntektPeriode> hentKontrollertInntektTidslinje(
        TilkjentYtelseRepository tilkjentYtelseRepository, Long behandlingId) {
        return tilkjentYtelseRepository.hentKontrollertInntektPerioder(behandlingId)
            .stream()
            .flatMap(it -> it.getPerioder().stream())
            .map(p -> new LocalDateTimeline<>(
                p.getPeriode().getFomDato(),
                p.getPeriode().getTomDato(),
                p)).reduce(LocalDateTimeline::crossJoin)
            .orElse(LocalDateTimeline.empty());
    }

    private static boolean erKontrollAvInntektMedTilkjentYtelse(DetaljertResultat r) {
        return r.harÅrsak(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT)
            && r.utbetalingsgrad().erSatt();
    }

}
