package no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell;

import java.util.List;

import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.grunnlag.inntekt.Arbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.grunnlag.inntekt.Refusjonskrav;

public interface AndelGradering {

    AktivitetStatusV2 getAktivitetStatus();

    List<Gradering> getGraderinger();

    boolean erNyAktivitet();

    List<Refusjonskrav> getGyldigeRefusjonskrav();

    Arbeidsforhold getArbeidsforhold();
}
