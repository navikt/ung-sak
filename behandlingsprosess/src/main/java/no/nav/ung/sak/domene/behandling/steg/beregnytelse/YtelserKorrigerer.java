package no.nav.ung.sak.domene.behandling.steg.beregnytelse;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.ytelse.KorrigertYtelseÅrsak;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.KorrigertYtelseVerdi;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseVerdi;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;

public class YtelserKorrigerer {

    public static final BigDecimal AVVIK_NEDRE_GRENSE = BigDecimal.valueOf(-0.5);

    /**
     * Ufører korrigering basert på avrundingsfeil fra tidligere perioder. Utbetaler som en korrigert dagsats i siste virkedag på ytelsen dersom avrundingsfeilen er større enn 0.5
     *
     * @param ytelseTidslinje        Beregnet ytelse tidslinje
     * @param godkjentUttakTidslinje
     * @return
     */
    public static LocalDateTimeline<KorrigertYtelseVerdi> korrigerYtelse(LocalDateTimeline<TilkjentYtelseVerdi> ytelseTidslinje, LocalDateTimeline<Boolean> godkjentUttakTidslinje) {
        var harBeregnetAllePerioder = godkjentUttakTidslinje.disjoint(ytelseTidslinje).isEmpty();
        if (harBeregnetAllePerioder) {
            var totaltAvvik = ytelseTidslinje.toSegments().stream().map(it -> BigDecimal.valueOf(it.getValue().avvikGrunnetAvrunding()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            // Dersom det totale avvike er er mindre enn 0.5, så vil ikke korrigeringen medføre endret utbetaling siden dette vil avrundes til 0
            // Dersom avviket er positivt ønsker vi ikke å korrigere (bruker har fått for mye grunnet avrundingsfeil)
            if (totaltAvvik.compareTo(AVVIK_NEDRE_GRENSE) <= 0) {
                var sisteVirkedag = finnSisteVirkedag(ytelseTidslinje);
                var sisteYtelseVerdi = ytelseTidslinje.intersection(new LocalDateInterval(sisteVirkedag, sisteVirkedag)).toSegments().first().getValue();
                var korrigertDagsats = sisteYtelseVerdi.dagsats().add(totaltAvvik.abs()).setScale(0, RoundingMode.HALF_UP);
                return new LocalDateTimeline<>(sisteVirkedag, sisteVirkedag, new KorrigertYtelseVerdi(korrigertDagsats, KorrigertYtelseÅrsak.KORRIGERING_AV_AVRUNDINGSFEIL));
            }
        }

        return LocalDateTimeline.empty();

    }

    private static LocalDate finnSisteVirkedag(LocalDateTimeline<TilkjentYtelseVerdi> ytelseTidslinje) {
        var d = ytelseTidslinje.getMaxLocalDate();
        while (d.getDayOfWeek().equals(DayOfWeek.SATURDAY) || d.getDayOfWeek().equals(DayOfWeek.SUNDAY)) {
            d = d.minusDays(1);
        }
        return d;
    }

}
