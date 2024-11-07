package no.nav.k9.sak.behandling.revurdering.felles;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Dagsatser {
    
    private static final BigDecimal TOTAL_ANDEL_NORMAL = BigDecimal.valueOf(300000);
    private static final BigDecimal TOTAL_ANDEL_OPPJUSTERT = BigDecimal.valueOf(350000);

    private BigDecimal dagsatsBruker;
    private BigDecimal dagsatsArbeidstaker;

    Dagsatser(boolean medOppjustertDagsat, boolean skalDeleAndelMellomArbeidsgiverOgBruker) {
        BigDecimal aktuellDagsats = medOppjustertDagsat ? TOTAL_ANDEL_OPPJUSTERT : TOTAL_ANDEL_NORMAL;
        this.dagsatsBruker = skalDeleAndelMellomArbeidsgiverOgBruker ? aktuellDagsats.divide(BigDecimal.valueOf(2), 0, RoundingMode.HALF_UP) : aktuellDagsats;
        this.dagsatsArbeidstaker = skalDeleAndelMellomArbeidsgiverOgBruker ? aktuellDagsats.divide(BigDecimal.valueOf(2), 0, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    public BigDecimal getDagsatsBruker() {
        return dagsatsBruker;
    }

    public BigDecimal getDagsatsArbeidstaker() {
        return dagsatsArbeidstaker;
    }
}
