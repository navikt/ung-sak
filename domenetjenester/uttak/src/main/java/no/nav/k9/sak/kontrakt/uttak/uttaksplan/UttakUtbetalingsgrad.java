package no.nav.k9.sak.kontrakt.uttak.uttaksplan;

import java.math.BigDecimal;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class UttakUtbetalingsgrad implements Comparable<UttakUtbetalingsgrad> {

    @JsonProperty(value = "arbeidsforhold", required = true)
    @NotNull
    @Valid
    private UttakArbeidsforhold arbeidsforhold;

    @JsonProperty(value = "utbetalingsgrad", required = true)
    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    private BigDecimal utbetalingsgrad;

    public UttakUtbetalingsgrad(@JsonProperty(value = "arbeidsforhold", required = true) @NotNull @Valid UttakArbeidsforhold arbeidsforhold,
                                @JsonProperty(value = "utbetalingsgrad", required = true) @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal utbetalingsgrad) {
        this.arbeidsforhold = Objects.requireNonNull(arbeidsforhold, "arbeidsforhold");
        this.utbetalingsgrad = Objects.requireNonNull(utbetalingsgrad, "utbetalingsgrad");
    }

    public BigDecimal getUtbetalingsgrad() {
        return utbetalingsgrad;
    }

    public UttakArbeidsforhold getArbeidsforhold() {
        return arbeidsforhold;
    }

    @Override
    public int compareTo(UttakUtbetalingsgrad o) {
        return this.arbeidsforhold.compareTo(o.arbeidsforhold);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<arbeidsforhold" + arbeidsforhold + ", utbetalingsgrad=" + utbetalingsgrad + ">";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof UttakUtbetalingsgrad))
            return false;
        var other = (UttakUtbetalingsgrad) obj;
        return Objects.equals(this.arbeidsforhold, other.arbeidsforhold);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arbeidsforhold);
    }
}
