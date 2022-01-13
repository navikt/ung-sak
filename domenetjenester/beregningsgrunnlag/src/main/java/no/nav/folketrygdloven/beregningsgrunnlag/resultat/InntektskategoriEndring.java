package no.nav.folketrygdloven.beregningsgrunnlag.resultat;

import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;

public class InntektskategoriEndring {

    private final Inntektskategori fraVerdi;
    private final Inntektskategori tilVerdi;

    public InntektskategoriEndring(Inntektskategori fraVerdi, Inntektskategori tilVerdi) {
        this.fraVerdi = fraVerdi;
        this.tilVerdi = tilVerdi;
    }

    public Inntektskategori getFraVerdi() {
        return fraVerdi;
    }

    public Inntektskategori getTilVerdi() {
        return tilVerdi;
    }

}
