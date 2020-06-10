package no.nav.k9.sak.ytelse.frisinn.beregningsgrunnlag;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.k9.sak.domene.typer.tid.ÅpenDatoIntervallEntitet;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ErEndringIBeregningFRISINN {
    // Alle måneder er sin egen søknadsperiode for frisinn, med unntak av mars som ble behandlet sammen med april
    private static final List<YearMonth> SPESIAL_BEHANDLING = Arrays.asList(YearMonth.of(2020,3), YearMonth.of(2020,4));

    private ErEndringIBeregningFRISINN() {
        // Skjuler default
    }

    public static boolean vurder(Optional<Beregningsgrunnlag> revurderingsGrunnlag, Optional<Beregningsgrunnlag> originaltGrunnlag, LocalDate førsteUttaksdag) {
        if (revurderingsGrunnlag.isEmpty()) {
            return originaltGrunnlag.isPresent();
        }

        List<BeregningsgrunnlagPeriode> originalePerioder = originaltGrunnlag.map(Beregningsgrunnlag::getBeregningsgrunnlagPerioder).orElse(Collections.emptyList());
        List<BeregningsgrunnlagPeriode> revurderingsPerioder = revurderingsGrunnlag.map(Beregningsgrunnlag::getBeregningsgrunnlagPerioder).orElse(Collections.emptyList());

        int counter =  0;
        while (counter < originalePerioder.size()) {
            BeregningsgrunnlagPeriode periode = originalePerioder.get(counter);
            if (!periode.getBeregningsgrunnlagPeriodeFom().isBefore(førsteUttaksdag)) {
                long utbetaling = utbetalingIPerioden(periode);
                long utbetalingNestePeriode = 0;
                boolean skalVurderesMedNestePeriode = nestePeriodeErISammeSøknadsperiode(periode, counter, originalePerioder);
                if (skalVurderesMedNestePeriode) {
                    counter++;
                    BeregningsgrunnlagPeriode nestePeriode = originalePerioder.get(counter);
                    utbetalingNestePeriode = utbetalingIPerioden(nestePeriode);
                }
                long utbetalingIOrginalSøknadsperiode = utbetaling + utbetalingNestePeriode;
                long utbetalingIRevurderingSøknadsperiode = finnUtbetalingForRevurdering(periode, revurderingsPerioder);
                if (utbetalingIOrginalSøknadsperiode > utbetalingIRevurderingSøknadsperiode) {
                    return true;
                }

            }
        }
        return false;
    }

    private static long finnUtbetalingForRevurdering(BeregningsgrunnlagPeriode periode, List<BeregningsgrunnlagPeriode> revurderingsPerioder) {
        Optional<BeregningsgrunnlagPeriode> matchetRevurderingsperiode = revurderingsPerioder.stream()
            .filter(p -> p.getPeriode().inkluderer(periode.getBeregningsgrunnlagPeriodeFom()))
            .findFirst();
        if (matchetRevurderingsperiode.isEmpty()) {
            return 0L;
        }
        BeregningsgrunnlagPeriode revurderingPeriode = matchetRevurderingsperiode.get();
        if (SPESIAL_BEHANDLING.contains(YearMonth.from(revurderingPeriode.getBeregningsgrunnlagPeriodeTom()))) {
            List<BeregningsgrunnlagPeriode> alleRevurderingsperioderISammeSøkandsperiode = revurderingsPerioder.stream()
                .filter(p -> SPESIAL_BEHANDLING.contains(YearMonth.from(p.getBeregningsgrunnlagPeriodeTom())))
                .collect(Collectors.toList());
            return finnUtbetalingForAllePerioder(alleRevurderingsperioderISammeSøkandsperiode);
        } else {
            List<BeregningsgrunnlagPeriode> allePerioderISammemåned = revurderingsPerioder.stream()
                .filter(p -> YearMonth.from(p.getBeregningsgrunnlagPeriodeTom()).equals(YearMonth.from(periode.getBeregningsgrunnlagPeriodeTom())))
                .collect(Collectors.toList());
            return finnUtbetalingForAllePerioder(allePerioderISammemåned);
        }
    }

    private static long finnUtbetalingForAllePerioder(List<BeregningsgrunnlagPeriode> perioder) {
        return perioder.stream()
            .map(p -> BigDecimal.valueOf(utbetalingIPerioden(p)))
            .reduce(BigDecimal::add)
            .orElse(BigDecimal.ZERO).longValue();
    }
    private static boolean nestePeriodeErISammeSøknadsperiode(BeregningsgrunnlagPeriode periode, int counter, List<BeregningsgrunnlagPeriode> revurderingsPerioder) {
        if (counter < revurderingsPerioder.size() - 1) {
            BeregningsgrunnlagPeriode nestePeriode = revurderingsPerioder.get(counter + 1);
            YearMonth nesteTom = YearMonth.from(nestePeriode.getBeregningsgrunnlagPeriodeTom());
            YearMonth gjeldendeTom = YearMonth.from(periode.getBeregningsgrunnlagPeriodeTom());
            boolean sammeÅrOgMåned = nesteTom.equals(gjeldendeTom);

            // Hvis søkt periode er i mars skal den alltid vurderes sammen med neste periode.
            // Dette fordi alle søknader går fra måned til måned, untatt første mulige søknadsperiode som startet i mars og gikk til april
            boolean førsteTomErIMars2020 = gjeldendeTom.equals(YearMonth.of(2020,3));

            return sammeÅrOgMåned || førsteTomErIMars2020;
        }
        return false;
    }

    private static long utbetalingIPerioden(BeregningsgrunnlagPeriode periode) {
        if (periode.getDagsats() == null) {
            return 0;
        }
        BigDecimal dagsats = BigDecimal.valueOf(periode.getDagsats());
        BigDecimal antallArbeidsdager = BigDecimal.valueOf(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(periode.getBeregningsgrunnlagPeriodeFom(), periode.getBeregningsgrunnlagPeriodeTom()).antallArbeidsdager());
        return dagsats.multiply(antallArbeidsdager).longValue();
    }
}
