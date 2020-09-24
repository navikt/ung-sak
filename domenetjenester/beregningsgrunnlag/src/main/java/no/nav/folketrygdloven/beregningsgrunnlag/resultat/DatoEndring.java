package no.nav.folketrygdloven.beregningsgrunnlag.resultat;

import java.time.LocalDate;

public class DatoEndring {

    private LocalDate fraVerdi;
    private LocalDate tilVerdi;

    public DatoEndring(LocalDate fraVerdi, LocalDate tilVerdi) {
        this.fraVerdi = fraVerdi;
        this.tilVerdi = tilVerdi;
    }

    public LocalDate getFraVerdi() {
        return fraVerdi;
    }

    public LocalDate getTilVerdi() {
        return tilVerdi;
    }
}
