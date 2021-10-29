package no.nav.folketrygdloven.beregningsgrunnlag.resultat;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public class OppdaterBeregningsgrunnlagResultat {

    private LocalDate skjæringstidspunkt;
    private UUID referanse;
    private BeregningsgrunnlagEndring beregningsgrunnlagEndring;
    private FaktaOmBeregningVurderinger faktaOmBeregningVurderinger;
    private VarigEndretNæringVurdering varigEndretNæringVurdering;

    public OppdaterBeregningsgrunnlagResultat(BeregningsgrunnlagEndring beregningsgrunnlagEndring,
                                              FaktaOmBeregningVurderinger faktaOmBeregningVurderinger,
                                              VarigEndretNæringVurdering varigEndretNæringVurdering,
                                              UUID referanse) {
        this.beregningsgrunnlagEndring = beregningsgrunnlagEndring;
        this.faktaOmBeregningVurderinger = faktaOmBeregningVurderinger;
        this.referanse = referanse;
        this.varigEndretNæringVurdering = varigEndretNæringVurdering;
    }

    public Optional<BeregningsgrunnlagEndring> getBeregningsgrunnlagEndring() {
        return Optional.ofNullable(beregningsgrunnlagEndring);
    }

    public Optional<FaktaOmBeregningVurderinger> getFaktaOmBeregningVurderinger() {
        return Optional.ofNullable(faktaOmBeregningVurderinger);
    }

    public Optional<VarigEndretNæringVurdering> getVarigEndretNæringVurdering() {
        return Optional.ofNullable(varigEndretNæringVurdering);
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
