package no.nav.k9.sak.kontrakt.uttak;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class UttakUtbetalingsgradOmsorgspenger {

    @JsonProperty(value = "utbetalingsgrad", required = true)
    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    private BigDecimal utbetalingsgrad;

    public UttakUtbetalingsgradOmsorgspenger(@JsonProperty(value = "utbetalingsgrad", required = true) @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal utbetalingsgrad) {
        this.utbetalingsgrad = Objects.requireNonNull(utbetalingsgrad, "utbetalingsgrad");
    }

    public BigDecimal getUtbetalingsgrad() {
        return utbetalingsgrad;
    }


    @Override
    public String toString() {
        return getClass().getSimpleName() + "<utbetalingsgrad=" + utbetalingsgrad + ">";
    }

}
