package no.nav.ung.ytelse.aktivitetspenger.beregning.minsteytelse;

import no.nav.ung.ytelse.aktivitetspenger.beregning.beste.Beregningsgrunnlag;

import java.math.BigDecimal;

public record AktivitetspengerSatser(
    AktivitetspengerSatsGrunnlag satsGrunnlag,
    Beregningsgrunnlag beregningsgrunnlag
) {
    public boolean erBeregningsgrunnlagStørreEnnMinsteytelse() {
        return beregningsgrunnlag.getBeregnetRedusertPrAar().compareTo(satsGrunnlag.minsteytelse()) > 0;
    }

    public BigDecimal getGrunnsats() {
        return erBeregningsgrunnlagStørreEnnMinsteytelse() ? beregningsgrunnlag.getDagsats() : satsGrunnlag.dagsats();
    }
}
