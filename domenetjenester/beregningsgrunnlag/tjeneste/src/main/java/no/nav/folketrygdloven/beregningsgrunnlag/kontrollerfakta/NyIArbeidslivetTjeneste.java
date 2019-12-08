package no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta;

import java.util.Collections;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.OppgittEgenNæring;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjening;

public class NyIArbeidslivetTjeneste {

    private NyIArbeidslivetTjeneste() {
        // Skjul
    }

    public static boolean erNyIArbeidslivetMedAktivitetStatusSN(BeregningsgrunnlagEntitet beregningsgrunnlag, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        boolean erSN = beregningsgrunnlag.getBeregningsgrunnlagPerioder().stream()
            .flatMap(periode -> periode.getBeregningsgrunnlagPrStatusOgAndelList().stream())
            .anyMatch(andel -> andel.getAktivitetStatus().erSelvstendigNæringsdrivende());
        return erSN
            && iayGrunnlag.getOppgittOpptjening()
            .map(OppgittOpptjening::getEgenNæring)
            .orElse(Collections.emptyList())
            .stream()
            .anyMatch(OppgittEgenNæring::getNyIArbeidslivet);
    }
}
