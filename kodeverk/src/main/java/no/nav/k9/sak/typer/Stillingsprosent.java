package no.nav.k9.sak.typer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

import no.nav.k9.kodeverk.api.IndexKey;

/**
 * Stillingsprosent slik det er oppgitt i arbeidsavtalen
 */
@JsonFormat(shape = JsonFormat.Shape.STRING)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class Stillingsprosent implements IndexKey {

    private static final RoundingMode AVRUNDINGSMODUS = RoundingMode.HALF_EVEN;

    public static final Stillingsprosent ZERO = new Stillingsprosent(0);

    public static final Stillingsprosent HUNDRED = new Stillingsprosent(100);

    @JsonValue
    @DecimalMin("0.00")
    @DecimalMax("500.00")
    private BigDecimal verdi;

    protected Stillingsprosent() {
        //
    }

    @JsonCreator
    public Stillingsprosent(BigDecimal verdi) {
        this.verdi = verdi == null ? null : verdi;
    }

    // Beleilig å kunne opprette gjennom int
    public Stillingsprosent(Integer verdi) {
        this(new BigDecimal(verdi));
    }

    // Beleilig å kunne opprette gjennom string
    public Stillingsprosent(String verdi) {
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
        Stillingsprosent other = (Stillingsprosent) obj;
        return Objects.equals(skalertVerdi(), other.skalertVerdi());
    }

    @Override
    public int hashCode() {
        return Objects.hash(skalertVerdi());
    }

    @Override
    public String toString() {
        return "Stillingsprosent{" +
            "verdi=" + verdi +
            ", skalertVerdi=" + skalertVerdi() +
            '}';
    }

    public boolean erNulltall() {
        return verdi != null && verdi.intValue() == 0;
    }
}
