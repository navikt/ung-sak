package no.nav.ung.ytelse.aktivitetspenger.beregning;

import java.math.BigDecimal;

public record AktivitetspengerBeregnetSats(BigDecimal dagsats, int dagsatsBarnetillegg) {

    public BigDecimal totalDagsats() {
        return dagsats.add(BigDecimal.valueOf(dagsatsBarnetillegg));
    }
}
