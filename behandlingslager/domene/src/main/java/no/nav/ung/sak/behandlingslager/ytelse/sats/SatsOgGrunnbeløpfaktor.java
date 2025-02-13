package no.nav.ung.sak.behandlingslager.ytelse.sats;

import no.nav.ung.kodeverk.ungdomsytelse.sats.UngdomsytelseSatsType;
import no.nav.ung.sak.behandlingslager.behandling.sporing.Sporingsverdi;

import java.math.BigDecimal;
import java.util.Objects;

public final class SatsOgGrunnbeløpfaktor extends Sporingsverdi {
    private final UngdomsytelseSatsType satstype;
    private final BigDecimal grunnbeløpFaktor;

    public SatsOgGrunnbeløpfaktor(UngdomsytelseSatsType satstype, BigDecimal grunnbeløpFaktor) {
        this.satstype = satstype;
        this.grunnbeløpFaktor = grunnbeløpFaktor;
    }

    @Override
    public String toString() {
        return "SatsOgGrunnbeløpfaktor{" +
            "satstype=" + satstype +
            ", grunnbeløpFaktor=" + grunnbeløpFaktor +
            '}';
    }

    @Override
    public String tilRegelVerdi() {
        return this.toString();
    }

    public UngdomsytelseSatsType satstype() {
        return satstype;
    }

    public BigDecimal grunnbeløpFaktor() {
        return grunnbeløpFaktor;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (SatsOgGrunnbeløpfaktor) obj;
        return Objects.equals(this.satstype, that.satstype) &&
            Objects.equals(this.grunnbeløpFaktor, that.grunnbeløpFaktor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(satstype, grunnbeløpFaktor);
    }

}
