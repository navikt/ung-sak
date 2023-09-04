package no.nav.k9.sak.kontrakt.uttak.overstyring;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class OverstyrUttakUtbetalingsgradDto {


    @JsonProperty(value = "arbeidsforhold", required = true)
    @NotNull
    private OverstyrUttakArbeidsforholdDto arbeidsforhold;

    @JsonProperty(value = "utbetalingsgrad")
    @NotNull
    @DecimalMin("0")
    @DecimalMax("100")
    private BigDecimal utbetalingsgrad;

    public OverstyrUttakUtbetalingsgradDto() {
        //
    }

    public OverstyrUttakUtbetalingsgradDto(OverstyrUttakArbeidsforholdDto arbeidsforhold, BigDecimal utbetalingsgrad) {
        this.arbeidsforhold = arbeidsforhold;
        this.utbetalingsgrad = utbetalingsgrad;
    }

    public OverstyrUttakArbeidsforholdDto getArbeidsforhold() {
        return arbeidsforhold;
    }

    public BigDecimal getUtbetalingsgrad() {
        return utbetalingsgrad;
    }
}
