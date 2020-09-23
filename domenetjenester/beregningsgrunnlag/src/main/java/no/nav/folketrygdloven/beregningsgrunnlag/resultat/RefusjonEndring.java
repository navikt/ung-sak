package no.nav.folketrygdloven.beregningsgrunnlag.resultat;

import java.math.BigDecimal;

public class RefusjonEndring {

    private BigDecimal fraRefusjon;
    private BigDecimal tilRefusjon;

    public RefusjonEndring(BigDecimal fraRefusjon, BigDecimal tilRefusjon) {
        this.fraRefusjon = fraRefusjon;
        this.tilRefusjon = tilRefusjon;
    }

    public BigDecimal getFraRefusjon() {
        return fraRefusjon;
    }

    public BigDecimal getTilRefusjon() {
        return tilRefusjon;
    }

}
