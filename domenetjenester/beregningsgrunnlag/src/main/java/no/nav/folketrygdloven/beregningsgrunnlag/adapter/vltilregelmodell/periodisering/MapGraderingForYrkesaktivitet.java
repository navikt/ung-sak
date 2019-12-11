package no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.periodisering;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AndelGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AndelGradering.Gradering;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.Periode;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;

class MapGraderingForYrkesaktivitet {
    private MapGraderingForYrkesaktivitet() {
        // skjul public constructor
    }

    static List<no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.Gradering> mapGraderingForYrkesaktivitet(Collection<AndelGradering> andelGraderinger, Yrkesaktivitet ya) {
        List<Gradering> graderingList = andelGraderinger.stream()
            .filter(gradering -> gradering.gjelderFor(ya.getArbeidsgiver(), ya.getArbeidsforholdRef()))
            .flatMap(g -> g.getGraderinger().stream())
            .collect(Collectors.toList());
        return mapGraderingPerioder(graderingList);
    }
    
    private static List<no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.Gradering> mapGraderingPerioder(List<Gradering> graderingList) {
        return graderingList.stream()
            .map(gradering -> new no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.Gradering(
                Periode.of(gradering.getPeriode().getFomDato(), gradering.getPeriode().getTomDato()),
                gradering.getArbeidstidProsent()))
            .collect(Collectors.toList());
    }
}
