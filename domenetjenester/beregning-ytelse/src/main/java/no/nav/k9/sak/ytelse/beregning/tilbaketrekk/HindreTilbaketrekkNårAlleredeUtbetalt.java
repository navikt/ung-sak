package no.nav.k9.sak.ytelse.beregning.tilbaketrekk;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.ytelse.beregning.regelmodell.Beregningsresultat;

@ApplicationScoped
public class HindreTilbaketrekkNårAlleredeUtbetalt {

    @Inject
    HindreTilbaketrekkNårAlleredeUtbetalt() {
        // for CDI proxy
    }

    /**
     * Vi har utbetalt beregningsresultat (uten endringsdato eller feriepenger)
     *
     * @param beregningsgrunnlagTY {@link Beregningsresultat} basert på {@link Beregningsgrunnlag}et
     * @param tidslinje            {@link LocalDateTimeline} tidslinje for å sammeligne utbetalt og beregningsgrunnlag-versjonen av tilkjent ytelse
     * @return {@link Beregningsresultat}et vi ønsker å utbetale
     */
    public BeregningsresultatEntitet reberegn(BeregningsresultatEntitet beregningsgrunnlagTY, LocalDateTimeline<BRAndelSammenligning> tidslinje) {
        // Map til regelmodell

        BeregningsresultatEntitet utbetaltTY = BeregningsresultatEntitet.builder()
            .medRegelSporing(beregningsgrunnlagTY.getRegelSporing())
            .medRegelInput(beregningsgrunnlagTY.getRegelInput())
            .build();

        for (var segment : tidslinje) {
            HindreTilbaketrekkBeregningsresultatPeriode.omfordelPeriodeVedBehov(utbetaltTY, segment);
        }
        return utbetaltTY;
    }

}
