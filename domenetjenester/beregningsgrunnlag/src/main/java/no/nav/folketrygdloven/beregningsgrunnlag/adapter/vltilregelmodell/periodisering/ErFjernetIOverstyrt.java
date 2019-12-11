package no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.periodisering;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetAggregatEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.Periode;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;

public class ErFjernetIOverstyrt {
    private ErFjernetIOverstyrt() {
        // skjul public constructor
    }

    public static boolean erFjernetIOverstyrt(YrkesaktivitetFilter filter,
                                              Yrkesaktivitet yrkesaktivitet,
                                              BeregningAktivitetAggregatEntitet aktivitetAggregatEntitet,
                                              LocalDate skjæringstidspunktBeregning) {

        List<Periode> ansettelsesPerioder = filter.getAnsettelsesPerioder(yrkesaktivitet).stream()
            .map(aa -> new Periode(aa.getPeriode().getFomDato(), aa.getPeriode().getTomDato()))
            .filter(periode -> !periode.getTom().isBefore(skjæringstidspunktBeregning.minusDays(1)))
            .collect(Collectors.toList());
        if (erAktivDagenFørSkjæringstidspunktet(skjæringstidspunktBeregning, ansettelsesPerioder)) {
            return erFjernet(yrkesaktivitet, aktivitetAggregatEntitet);
        }
        return false;
    }

    private static boolean erFjernet(Yrkesaktivitet yrkesaktivitet, BeregningAktivitetAggregatEntitet beregningAktivitetAggregat) {
        return beregningAktivitetAggregat.getBeregningAktiviteter().stream()
            .noneMatch(beregningAktivitet -> beregningAktivitet.gjelderFor(yrkesaktivitet.getArbeidsgiver(), yrkesaktivitet.getArbeidsforholdRef()));
    }

    private static boolean erAktivDagenFørSkjæringstidspunktet(LocalDate skjæringstidspunktBeregning, List<Periode> ansettelsesPerioder) {
        return ansettelsesPerioder.stream().anyMatch(periode -> periode.inneholder(skjæringstidspunktBeregning.minusDays(1)));
    }
}
