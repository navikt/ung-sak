package no.nav.ung.sak.typer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.ung.kodeverk.api.IndexKey;

/**
 * Beløp representerer kombinasjon av kroner og øre på standardisert format
 */
@JsonFormat(shape = JsonFormat.Shape.STRING)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class Beløp implements IndexKey {
    public static final Beløp ZERO = new Beløp(BigDecimal.ZERO);
    private static final RoundingMode AVRUNDINGSMODUS = RoundingMode.HALF_EVEN;

    @JsonProperty(value = "verdi")
    @Digits(integer = 8, fraction = 4) // litt romslig?
    @DecimalMin("-10000000.00") // burde ikke tillate negative beløp eller null. bør ha forhold til credit/debit i stedet.
    @DecimalMax("10000000.00")
    private BigDecimal verdi;

    protected Beløp() {
        //
    }

    @JsonCreator
    public Beløp(@JsonProperty(value = "verdi") BigDecimal verdi) {
        this.verdi = verdi; // godtar dessverre null her :-(
    }

    // Beleilig å kunne opprette gjennom int
    public Beløp(Integer verdi) {
        this.verdi = verdi == null ? null : new BigDecimal(verdi);
    }

    // Beleilig å kunne opprette gjennom int
    public Beløp(Number verdi) {
        this.verdi = verdi == null ? null : new BigDecimal(verdi.toString());
    }

    // Beleilig å kunne opprette gjennom string
    public Beløp(String verdi) {
        this.verdi = verdi == null ? null : new BigDecimal(verdi);
    }

    private BigDecimal skalertVerdi() {
        return verdi == null ? null : verdi.setScale(2, AVRUNDINGSMODUS);
    }

    @Override
    public String getIndexKey() {
        BigDecimal skalertVerdi = skalertVerdi();
        return skalertVerdi != null ? skalertVerdi.toString() : null;
    }

    public BigDecimal getVerdi() {
        return verdi;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null || !getClass().equals(obj.getClass())) {
            return false;
        }
        Beløp other = (Beløp) obj;
        return Objects.equals(skalertVerdi(), other.skalertVerdi());
    }

    @Override
    public int hashCode() {
        return Objects.hash(skalertVerdi());
    }

    @Override
    public String toString() {
        return "Beløp{" +
            "verdi=" + verdi +
            ", skalertVerdi=" + skalertVerdi() +
            '}';
    }

    public int compareTo(Beløp annetBeløp) {
        return verdi.compareTo(annetBeløp.getVerdi());
    }

    public boolean erNullEllerNulltall() {
        return verdi == null || erNulltall();
    }

    public boolean erNulltall() {
        return verdi != null && compareTo(Beløp.ZERO) == 0;
    }

    public Beløp multipliser(int multiplicand) {
        return new Beløp(this.verdi.multiply(BigDecimal.valueOf(multiplicand)));
    }

    public Beløp adder(Beløp augend) {
        return new Beløp(this.verdi.add(augend.getVerdi()));
    }
}
