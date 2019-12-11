package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.finn.unleash.Unleash;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.resultat.Beregningsgrunnlag;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.Beregningsresultat;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

@ApplicationScoped
public class HindreTilbaketrekkNårAlleredeUtbetalt {
    private static final String TOGGLE = "fpsak.match.beregningsresultat";
    
    private Unleash unleash;
    
    HindreTilbaketrekkNårAlleredeUtbetalt() {
        // for CDI proxy
    }

    @Inject
    public HindreTilbaketrekkNårAlleredeUtbetalt(Unleash unleash) {
        this.unleash = unleash;
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

        for (LocalDateSegment<BRAndelSammenligning> segment : tidslinje.toSegments()) {
            HindreTilbaketrekkBeregningsresultatPeriode.omfordelPeriodeVedBehov(utbetaltTY, segment, unleash.isEnabled(TOGGLE));
        }
        return utbetaltTY;
    }

}
