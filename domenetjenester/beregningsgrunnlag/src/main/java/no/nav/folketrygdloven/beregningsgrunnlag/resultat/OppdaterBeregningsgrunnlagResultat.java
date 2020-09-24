package no.nav.folketrygdloven.beregningsgrunnlag.resultat;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public class OppdaterBeregningsgrunnlagResultat {

    private LocalDate skjæringstidspunkt;
    private UUID referanse;
    private BeregningsgrunnlagEndring beregningsgrunnlagEndring;
    private FaktaOmBeregningVurderinger faktaOmBeregningVurderinger;

    public OppdaterBeregningsgrunnlagResultat(BeregningsgrunnlagEndring beregningsgrunnlagEndring, FaktaOmBeregningVurderinger faktaOmBeregningVurderinger, UUID referanse) {
        this.beregningsgrunnlagEndring = beregningsgrunnlagEndring;
        this.faktaOmBeregningVurderinger = faktaOmBeregningVurderinger;
        this.referanse = referanse;
    }

    public Optional<BeregningsgrunnlagEndring> getBeregningsgrunnlagEndring() {
        return Optional.ofNullable(beregningsgrunnlagEndring);
    }

    public Optional<FaktaOmBeregningVurderinger> getFaktaOmBeregningVurderinger() {
        return Optional.ofNullable(faktaOmBeregningVurderinger);
    }

    public LocalDate getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    public UUID getReferanse() {
        return referanse;
    }


    public void setSkjæringstidspunkt(LocalDate skjæringstidspunkt) {
        this.skjæringstidspunkt = skjæringstidspunkt;
    }

}
