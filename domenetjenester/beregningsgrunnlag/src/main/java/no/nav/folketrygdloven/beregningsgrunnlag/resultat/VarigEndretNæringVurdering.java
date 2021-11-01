package no.nav.folketrygdloven.beregningsgrunnlag.resultat;

public class VarigEndretNæringVurdering {

    private ToggleEndring erVarigEndretNæringEndring;
    private ToggleEndring erNyoppstartetNæringEndring;


    public VarigEndretNæringVurdering() {
    }

    public VarigEndretNæringVurdering(ToggleEndring erVarigEndretNæringEndring, ToggleEndring erNyoppstartetNæringEndring) {
        this.erVarigEndretNæringEndring = erVarigEndretNæringEndring;
        this.erNyoppstartetNæringEndring = erNyoppstartetNæringEndring;
    }

    public ToggleEndring getErVarigEndretNæringEndring() {
        return erVarigEndretNæringEndring;
    }

    public ToggleEndring getErNyoppstartetNæringEndring() {
        return erNyoppstartetNæringEndring;
    }
}
