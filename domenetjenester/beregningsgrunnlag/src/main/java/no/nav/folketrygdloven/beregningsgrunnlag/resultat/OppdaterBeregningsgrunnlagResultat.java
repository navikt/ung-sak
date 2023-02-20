package no.nav.folketrygdloven.beregningsgrunnlag.resultat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class OppdaterBeregningsgrunnlagResultat {

    private LocalDate skjæringstidspunkt;
    private final UUID referanse;
    private final BeregningsgrunnlagEndring beregningsgrunnlagEndring;
    private final List<BeregningAktivitetEndring> beregningAktivitetEndringer;
    private final FaktaOmBeregningVurderinger faktaOmBeregningVurderinger;
    private final VarigEndretNæringVurdering varigEndretNæringVurdering;
    private final RefusjonoverstyringEndring refusjonoverstyringEndring;

    public OppdaterBeregningsgrunnlagResultat(BeregningsgrunnlagEndring beregningsgrunnlagEndring,
                                              FaktaOmBeregningVurderinger faktaOmBeregningVurderinger,
                                              VarigEndretNæringVurdering varigEndretNæringVurdering,
                                              RefusjonoverstyringEndring refusjonoverstyringEndring, UUID referanse,
                                              List<BeregningAktivitetEndring> beregningAktivitetEndringer) {
        this.beregningsgrunnlagEndring = beregningsgrunnlagEndring;
        this.faktaOmBeregningVurderinger = faktaOmBeregningVurderinger;
        this.refusjonoverstyringEndring = refusjonoverstyringEndring;
        this.referanse = referanse;
        this.varigEndretNæringVurdering = varigEndretNæringVurdering;
        this.beregningAktivitetEndringer = beregningAktivitetEndringer;
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

    public Optional<RefusjonoverstyringEndring> getRefusjonoverstyringEndring() {
        return Optional.ofNullable(refusjonoverstyringEndring);
    }

    public List<BeregningAktivitetEndring> getBeregningAktivitetEndringer() {
        return beregningAktivitetEndringer;
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
