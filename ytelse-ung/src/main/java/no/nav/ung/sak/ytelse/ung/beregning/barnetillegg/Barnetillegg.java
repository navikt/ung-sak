package no.nav.ung.sak.ytelse.ung.beregning.barnetillegg;

import java.math.BigDecimal;

public record Barnetillegg(BigDecimal dagsats, int antallBarn) {

    @Override
    public String toString() {
        return "Barnetillegg{" +
            "dagsats=" + dagsats +
            ", antallBarn=" + antallBarn +
            '}';
    }
}
