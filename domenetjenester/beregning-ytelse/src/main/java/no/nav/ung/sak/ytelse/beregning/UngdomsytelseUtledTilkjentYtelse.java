package no.nav.ung.sak.ytelse.beregning;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Period;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
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
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.ytelse.DagsatsOgUtbetalingsgrad;

@Dependent
public class UngdomsytelseUtledTilkjentYtelse implements UtledTilkjentYtelse {

    private UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository;

    protected UngdomsytelseUtledTilkjentYtelse() {
        // for proxy
    }

    @Inject
    public UngdomsytelseUtledTilkjentYtelse(UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository) {
        this.ungdomsytelseGrunnlagRepository = ungdomsytelseGrunnlagRepository;
    }

    @Override
    public Optional<List<TilkjentYtelsePeriode>> utledTilkjentYtelsePerioder(Long behandlingId) {
        var ungdomsytelseGrunnlag = ungdomsytelseGrunnlagRepository.hentGrunnlag(behandlingId);
        var satsTidslinje = ungdomsytelseGrunnlag.map(UngdomsytelseGrunnlag::getSatsTidslinje).orElse(LocalDateTimeline.empty());
        var utbetalingsgradTidslinje = ungdomsytelseGrunnlag.map(UngdomsytelseGrunnlag::getUtbetalingsgradTidslinje).orElse(LocalDateTimeline.empty());

        var resultatTidslinje = satsTidslinje.intersection(utbetalingsgradTidslinje, sammenstillSatsOgGradering());

        if (resultatTidslinje.isEmpty()) {
            return Optional.empty();
        }

        // stopper periodisering her for å unngå 'evigvarende' ekspansjon -
        // tar første av potensielle maks datoer som berører intersection av de to tidslinjene.
        var minsteMaksDato = Stream.of(satsTidslinje.getMaxLocalDate(), utbetalingsgradTidslinje.getMaxLocalDate()).sorted().findFirst().orElseThrow();
        // Splitter på år, pga chk_br_andel_samme_aar constraint i database
        resultatTidslinje = resultatTidslinje.splitAtRegular(utbetalingsgradTidslinje.getMinLocalDate().withDayOfYear(1), minsteMaksDato, Period.ofYears(1));

        return Optional.of(resultatTidslinje.toSegments().stream()
            .filter(s -> s.getValue().utbetalingsgrad().compareTo(BigDecimal.ZERO) > 0) // Filterer ut perioder med ingen utbetalingsgrad.
            .map(p -> new TilkjentYtelsePeriode(DatoIntervallEntitet.fraOgMedTilOgMed(p.getFom(), p.getTom()),
                p.getValue().dagsats(),
                p.getValue().utbetalingsgrad())).toList());
    }

    @Override
    public LocalDateTimeline<DagsatsOgUtbetalingsgrad> utledTilkjentYtelseTidslinje(Long behandlingId) {
        return utledTilkjentYtelsePerioder(behandlingId)
            .stream()
            .flatMap(Collection::stream)
            .map(p -> new LocalDateTimeline<>(p.periode().getFomDato(), p.periode().getTomDato(), new DagsatsOgUtbetalingsgrad(p.dagsats(), p.utbetalingsgrad())))
            .reduce(LocalDateTimeline::crossJoin)
            .orElse(LocalDateTimeline.empty());
    }

    private static LocalDateSegmentCombinator<UngdomsytelseSatser, UngdomsytelseUttak, DagsatsOgUtbetalingsgrad> sammenstillSatsOgGradering() {
        return (di, lhs, rhs) ->
        {
            var dagsats = lhs.getValue().dagsats().multiply(rhs.getValue().utbetalingsgrad()).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            var dagsatsBarnetillegg = UngdomsytelseUttakAvslagsårsak.IKKE_NOK_DAGER.equals(rhs.getValue().avslagsårsak()) ? 0L : lhs.getValue().dagsatsBarnetillegg();
            return new LocalDateSegment<>(di,
                new DagsatsOgUtbetalingsgrad(dagsats.add(BigDecimal.valueOf(dagsatsBarnetillegg)), rhs.getValue().utbetalingsgrad()));
        };
    }

}
