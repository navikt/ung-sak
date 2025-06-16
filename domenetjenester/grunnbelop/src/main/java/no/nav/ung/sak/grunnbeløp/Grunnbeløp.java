package no.nav.ung.sak.grunnbeløp;

import java.math.BigDecimal;
import java.util.Objects;

public class Grunnbeløp {
    private BigDecimal verdi;

    public Grunnbeløp(BigDecimal verdi) {
        this.verdi = verdi;
    }

    public BigDecimal verdi() {
        return verdi;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Grunnbeløp) obj;
        return Objects.equals(this.verdi, that.verdi);
    }

    @Override
    public int hashCode() {
        return Objects.hash(verdi);
    }

    @Override
    public String toString() {
        return "Grunnbeløp[" +
            "verdi=" + verdi + ']';
    }

}
