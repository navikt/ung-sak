package no.nav.ung.ytelse.aktivitetspenger.beregning;

import no.nav.ung.ytelse.aktivitetspenger.beregning.beste.Beregningsgrunnlag;
import no.nav.ung.ytelse.aktivitetspenger.beregning.minstesats.AktivitetspengerSatsGrunnlag;

public record AktivitetspengerSatser(
    AktivitetspengerSatsGrunnlag satsGrunnlag,
    Beregningsgrunnlag beregningsgrunnlag
) {
    public GrunnsatsType utledGrunnsatsBenyttet() {
        return beregningsgrunnlag.getBeregnetRedusertPrAar().compareTo(satsGrunnlag.minsteytelse()) > 0
            ? GrunnsatsType.BEREGNINGSGRUNNLAG
            : GrunnsatsType.MINSTEYTELSE;
    }

    public AktivitetspengerBeregnetSats hentBeregnetSats() {
        var dagsats = utledGrunnsatsBenyttet() == GrunnsatsType.BEREGNINGSGRUNNLAG ?
            beregningsgrunnlag.getDagsats() :
            satsGrunnlag.dagsats();

        return new AktivitetspengerBeregnetSats(dagsats, satsGrunnlag.dagsatsBarnetillegg());
    }
}
