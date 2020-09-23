package no.nav.folketrygdloven.beregningsgrunnlag.resultat;

import no.nav.k9.sak.typer.Arbeidsgiver;

public class RefusjonskravGyldighetEndring {

    private ToggleEndring erGyldighetUtvidet;
    private Arbeidsgiver arbeidsgiver;


    public RefusjonskravGyldighetEndring(ToggleEndring erGyldighetUtvidet, Arbeidsgiver arbeidsgiver) {
        this.erGyldighetUtvidet = erGyldighetUtvidet;
        this.arbeidsgiver = arbeidsgiver;
    }

    public ToggleEndring getErGyldighetUtvidet() {
        return erGyldighetUtvidet;
    }

    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }
}
