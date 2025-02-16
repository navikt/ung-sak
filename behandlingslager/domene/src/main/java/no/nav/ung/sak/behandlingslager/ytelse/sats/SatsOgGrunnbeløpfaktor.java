package no.nav.ung.sak.behandlingslager.ytelse.sats;

import no.nav.ung.kodeverk.ungdomsytelse.sats.UngdomsytelseSatsType;

import java.math.BigDecimal;
import java.util.Objects;

public record SatsOgGrunnbeløpfaktor(UngdomsytelseSatsType satstype, BigDecimal grunnbeløpFaktor) {

    @Override
    public String toString() {
        return "SatsOgGrunnbeløpfaktor{" +
            "satstype=" + satstype +
            ", grunnbeløpFaktor=" + grunnbeløpFaktor +
            '}';
    }

}
