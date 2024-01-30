package no.nav.k9.sak.typer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import no.nav.k9.kodeverk.api.IndexKey;

/**
 * Prosentverdi
 */
@JsonFormat(shape = JsonFormat.Shape.STRING)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class Prosent implements IndexKey {

    private static final RoundingMode AVRUNDINGSMODUS = RoundingMode.HALF_EVEN;

    public static final Prosent ZERO = new Prosent(0);

    public static final Prosent HUNDRED = new Prosent(100);

    @JsonValue
    @DecimalMin("0.00")
    private BigDecimal verdi;

    protected Prosent() {
        //
    }

    @JsonCreator
    public Prosent(BigDecimal verdi) {
        this.verdi = verdi;
    }

    // Beleilig å kunne opprette gjennom int
    public Prosent(Integer verdi) {
        this(new BigDecimal(verdi));
    }

    // Beleilig å kunne opprette gjennom string
    public Prosent(String verdi) {
        this(new BigDecimal(verdi));
    }

    @Override
    public String getIndexKey() {
        return skalertVerdi().toString();
    }

    public BigDecimal getVerdi() {
        return verdi;
    }

    private BigDecimal skalertVerdi() {
        return verdi.setScale(2, AVRUNDINGSMODUS);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null || !getClass().equals(obj.getClass())) {
            return false;
        }
        Prosent other = (Prosent) obj;
        return skalertVerdi().compareTo(other.skalertVerdi()) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(skalertVerdi());
    }

    @Override
    public String toString() {
        return "Prosent{" +
            "verdi=" + verdi +
            ", skalertVerdi=" + skalertVerdi() +
            '}';
    }

    public boolean erNulltall() {
        return verdi != null && verdi.intValue() == 0;
    }
}
