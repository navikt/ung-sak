package no.nav.ung.sak.ytelse.beregning;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Period;
import java.util.stream.Stream;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.ungdomsytelse.uttak.UngdomsytelseUttakAvslagsårsak;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlag;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatser;
import no.nav.ung.sak.behandlingslager.ytelse.uttak.UngdomsytelseUttak;
import no.nav.ung.sak.ytelse.DagsatsOgUtbetalingsgrad;

@Dependent
public class UngdomsytelseTilkjentYtelseUtleder implements TilkjentYtelseUtleder {

    private final UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository;

    @Inject
    public UngdomsytelseTilkjentYtelseUtleder(UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository) {
        this.ungdomsytelseGrunnlagRepository = ungdomsytelseGrunnlagRepository;
    }

    @Override
    public LocalDateTimeline<DagsatsOgUtbetalingsgrad> utledTilkjentYtelseTidslinje(Long behandlingId) {
        var ungdomsytelseGrunnlag = ungdomsytelseGrunnlagRepository.hentGrunnlag(behandlingId);
        var satsTidslinje = ungdomsytelseGrunnlag.map(UngdomsytelseGrunnlag::getSatsTidslinje).orElse(LocalDateTimeline.empty());
        var utbetalingsgradTidslinje = ungdomsytelseGrunnlag.map(UngdomsytelseGrunnlag::getUtbetalingsgradTidslinje).orElse(LocalDateTimeline.empty());

        var resultatTidslinje = satsTidslinje.intersection(utbetalingsgradTidslinje, sammenstillSatsOgGradering());

        if (resultatTidslinje.isEmpty()) {
            return LocalDateTimeline.empty();
        }

        // stopper periodisering her for å unngå 'evigvarende' ekspansjon -
        // tar første av potensielle maks datoer som berører intersection av de to tidslinjene.
        var minsteMaksDato = Stream.of(satsTidslinje.getMaxLocalDate(), utbetalingsgradTidslinje.getMaxLocalDate()).sorted().findFirst().orElseThrow();
        // Splitter på år, pga chk_br_andel_samme_aar constraint i database
        resultatTidslinje = resultatTidslinje.splitAtRegular(utbetalingsgradTidslinje.getMinLocalDate().withDayOfYear(1), minsteMaksDato, Period.ofYears(1));

        return resultatTidslinje.filterValue(v -> v.utbetalingsgrad().compareTo(BigDecimal.ZERO) > 0);
    }

    private static LocalDateSegmentCombinator<UngdomsytelseSatser, UngdomsytelseUttak, DagsatsOgUtbetalingsgrad> sammenstillSatsOgGradering() {
        return (di, lhs, rhs) ->
        {
            var dagsats = lhs.getValue().dagsats().multiply(rhs.getValue().utbetalingsgrad()).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            var dagsatsBarnetillegg = UngdomsytelseUttakAvslagsårsak.IKKE_NOK_DAGER.equals(rhs.getValue().avslagsårsak()) ? 0L : lhs.getValue().dagsatsBarnetillegg();
            return new LocalDateSegment<>(di,
                new DagsatsOgUtbetalingsgrad(dagsats.add(BigDecimal.valueOf(dagsatsBarnetillegg)).setScale(0, RoundingMode.HALF_UP).longValue(), rhs.getValue().utbetalingsgrad().setScale(2, RoundingMode.HALF_UP)));
        };
    }

}
