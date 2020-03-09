package no.nav.k9.sak.kontrakt.beregningsresultat;

import java.math.BigDecimal;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.uttak.PeriodeResultatType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class UttakDto {

    public static class Builder {
        private PeriodeResultatType periodeResultatType;
        private BigDecimal utbetalingsgrad;

        private Builder() {
        }

        public UttakDto create() {
            String periodeResultatTypeString = periodeResultatType == null ? null : periodeResultatType.getKode();
            return new UttakDto(periodeResultatTypeString, utbetalingsgrad);
        }

        public Builder medPeriodeResultatType(PeriodeResultatType periodeResultatType) {
            this.periodeResultatType = periodeResultatType;
            return this;
        }

        public Builder medUtbetalingsgrad(BigDecimal utbetalingsgrad) {
            this.utbetalingsgrad = utbetalingsgrad;
            return this;
        }
    }

    @JsonProperty(value = "utbetalingsgrad", required = true)
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    @Digits(integer = 3, fraction = 2)
    @NotNull
    private BigDecimal utbetalingsgrad;

    @JsonProperty(value = "periodeResultatType", required = true)
    @NotNull
    @Size(max = 50)
    @Pattern(regexp = "^[\\p{Alnum}_\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String periodeResultatType;

    public UttakDto(String periodeResultatType, BigDecimal utbetalingsgrad) {
        this.periodeResultatType = periodeResultatType;
        this.utbetalingsgrad = utbetalingsgrad;
    }

    protected UttakDto() {
        //
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getPeriodeResultatType() {
        return periodeResultatType;
    }

    public BigDecimal getUtbetalingsgrad() {
        return utbetalingsgrad;
    }
}
