package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import java.util.List;

import no.nav.foreldrepenger.behandlingslager.Kopimaskin;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.fpsak.tidsserie.LocalDateSegment;

class HindreTilbaketrekkBeregningsresultatPeriode {

    private HindreTilbaketrekkBeregningsresultatPeriode() {
        // skjul public constructor
    }

    static BeregningsresultatPeriode omfordelPeriodeVedBehov(BeregningsresultatEntitet utbetaltTY, LocalDateSegment<BRAndelSammenligning> segment, boolean brukToggletMatchingAvAndeler) {
        int bgDagsats = segment.getValue().getBgAndeler().stream()
            .mapToInt(BeregningsresultatAndel::getDagsats)
            .sum();

        BeregningsresultatPeriode beregningsresultatPeriode = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(segment.getFom(), segment.getTom())
            .build(utbetaltTY);
        BRAndelSammenligning wrapper = segment.getValue();
        List<BeregningsresultatAndel> forrigeAndeler = wrapper.getForrigeAndeler();
        List<BeregningsresultatAndel> bgAndeler = wrapper.getBgAndeler();
        if (forrigeAndeler.isEmpty() || kunUtbetalingTilArbeidsgiver(forrigeAndeler)) {
            // ikke utbetalt tidligere: kopier bg-andeler
            bgAndeler.forEach(andel ->
                BeregningsresultatAndel.builder(Kopimaskin.deepCopy(andel))
                    .medDagsats(andel.getDagsats())
                    .medDagsatsFraBg(andel.getDagsatsFraBg())
                    .build(beregningsresultatPeriode)
            );
        } else {
            if (brukToggletMatchingAvAndeler) {
                List<BeregningsresultatAndel.Builder> builders = OmfordelUtbetaltYtelseV2.omfordel(forrigeAndeler, bgAndeler);
                builders.forEach(builder -> builder.build(beregningsresultatPeriode));
                postcondition(bgDagsats, beregningsresultatPeriode);

            } else {
                List<BeregningsresultatAndel.Builder> builders = OmfordelUtbetaltYtelse.omfordel(forrigeAndeler, bgAndeler);
                builders.forEach(builder -> builder.build(beregningsresultatPeriode));
                postcondition(bgDagsats, beregningsresultatPeriode);
            }
        }
        return beregningsresultatPeriode;
    }

    private static void postcondition(int bgDagsats, BeregningsresultatPeriode beregningsresultatPeriode) {
        int utbetDagsats = beregningsresultatPeriode.getBeregningsresultatAndelList().stream()
            .mapToInt(BeregningsresultatAndel::getDagsats)
            .sum();

        if (bgDagsats != utbetDagsats) {
            throw new IllegalStateException("Utviklerfeil: utbetDagsats er ulik bgDagsats");
        }
    }

    private static boolean kunUtbetalingTilArbeidsgiver(List<BeregningsresultatAndel> andeler) {
        return andeler.stream()
            .filter(andel -> andel.getDagsats() > 0)
            .noneMatch(BeregningsresultatAndel::erBrukerMottaker);
    }
}
