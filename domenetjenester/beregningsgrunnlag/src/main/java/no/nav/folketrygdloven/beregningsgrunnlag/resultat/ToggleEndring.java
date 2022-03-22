package no.nav.folketrygdloven.beregningsgrunnlag.resultat;

import java.util.Optional;

public class ToggleEndring {

    private final Boolean fraVerdi;
    private final Boolean tilVerdi;

    public ToggleEndring(Boolean fraVerdi, Boolean tilVerdi) {
        this.fraVerdi = fraVerdi;
        this.tilVerdi = tilVerdi;
    }

    public boolean erEndret() {
        return !tilVerdi.equals(fraVerdi);
    }

    public Boolean getFraVerdiEllerNull() {
        return fraVerdi;
    }

    public Optional<Boolean> getFraVerdi() {
        return Optional.ofNullable(fraVerdi);
    }

    public Boolean getTilVerdi() {
        return tilVerdi;
    }
}
