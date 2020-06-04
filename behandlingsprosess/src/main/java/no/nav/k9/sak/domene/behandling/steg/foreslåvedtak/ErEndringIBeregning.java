package no.nav.k9.sak.domene.behandling.steg.foreslåvedtak;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.k9.sak.domene.typer.tid.ÅpenDatoIntervallEntitet;

public class ErEndringIBeregning {

    private ErEndringIBeregning() {
        // hide
    }

    public static boolean vurderUgunst(Optional<Beregningsgrunnlag> revurderingsGrunnlag, Optional<Beregningsgrunnlag> originaltGrunnlag) {
        if (revurderingsGrunnlag.isEmpty()) {
            return originaltGrunnlag.isPresent();
        }

        List<BeregningsgrunnlagPeriode> originalePerioder = originaltGrunnlag.map(Beregningsgrunnlag::getBeregningsgrunnlagPerioder).orElse(Collections.emptyList());
        List<BeregningsgrunnlagPeriode> revurderingsPerioder = revurderingsGrunnlag.map(Beregningsgrunnlag::getBeregningsgrunnlagPerioder).orElse(Collections.emptyList());

        Set<LocalDate> allePeriodeDatoer = finnAlleEndringsdatoer(revurderingsPerioder, originalePerioder);

        if (allePeriodeDatoer.size() < 2) {
            throw new IllegalStateException("Antall endringsdatoer kan ikke vere lavere enn 2");
        }

        Iterator<LocalDate> periodeIterator = allePeriodeDatoer.iterator();
        LocalDate fom = periodeIterator.next();
        for (int i = 0 ; i < allePeriodeDatoer.size() - 2; i++) {

            LocalDate tom = periodeIterator.next();

            Optional<BeregningsgrunnlagPeriode> revurderingsperiode = finnPeriodeSomInkludererDato(revurderingsPerioder, fom);
            Optional<BeregningsgrunnlagPeriode> originalperiode = finnPeriodeSomInkludererDato(originalePerioder, fom);

            int antallArbeidsdager = ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(fom, tom).antallArbeidsdager();

            int totalUtbetalingIOverlappendePeriodeRevurdering = finnTotalUtbetalingForAntallDager(revurderingsperiode, antallArbeidsdager);
            int totalUtbetalingIOverlappendeOriginal = finnTotalUtbetalingForAntallDager(originalperiode, antallArbeidsdager);

            if (totalUtbetalingIOverlappendePeriodeRevurdering < totalUtbetalingIOverlappendeOriginal) {
                return true;
            }
            fom = tom;
        }
        return false;
    }

    private static int finnTotalUtbetalingForAntallDager(Optional<BeregningsgrunnlagPeriode> revurderingsperiode, int minsteAntallArbeidsdager) {
        return minsteAntallArbeidsdager * revurderingsperiode.map(BeregningsgrunnlagPeriode::getDagsats)
            .map(Long::intValue)
            .orElse(0);
    }

    private static Optional<BeregningsgrunnlagPeriode> finnPeriodeSomInkludererDato(List<BeregningsgrunnlagPeriode> revurderingsPerioder, LocalDate dato) {
        return revurderingsPerioder.stream().filter(p -> p.getPeriode().inkluderer(dato))
            .findFirst();
    }

    private static Set<LocalDate> finnAlleEndringsdatoer(List<BeregningsgrunnlagPeriode> revurderingsPerioder, List<BeregningsgrunnlagPeriode> originalePerioder) {
        Set<LocalDate> startDatoer = new HashSet<>();
        revurderingsPerioder.stream().map(BeregningsgrunnlagPeriode::getBeregningsgrunnlagPeriodeFom).forEach(startDatoer::add);
        originalePerioder.stream().map(BeregningsgrunnlagPeriode::getBeregningsgrunnlagPeriodeFom).forEach(startDatoer::add);
        revurderingsPerioder.stream().map(BeregningsgrunnlagPeriode::getBeregningsgrunnlagPeriodeTom).forEach(startDatoer::add);
        originalePerioder.stream().map(BeregningsgrunnlagPeriode::getBeregningsgrunnlagPeriodeTom).forEach(startDatoer::add);
        return startDatoer;
    }

}
