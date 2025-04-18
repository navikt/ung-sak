package no.nav.ung.sak.kontrakt.beregningsresultat;

import java.math.BigDecimal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.ung.kodeverk.uttak.UtfallType;
import no.nav.ung.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class UttakDto {

    @JsonProperty(value = "periode", required = true)
    @Valid
    @NotNull
    private Periode periode;

    @JsonProperty(value = "utbetalingsgrad", required = true)
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    @Digits(integer = 3, fraction = 2)
    @NotNull
    private BigDecimal utbetalingsgrad;

    @JsonProperty(value = "utfall", required = true)
    @NotNull
    @Valid
    private UtfallType utfall;

    public UttakDto() {
        // Deserialisering av JSON
    }

    public UttakDto(UtfallType utfall, BigDecimal utbetalingsgrad) {
        this.utfall = utfall;
        this.utbetalingsgrad = utbetalingsgrad;
    }

    public UttakDto(Periode periode, UtfallType utfallType, BigDecimal utbetalingsgrad) {
        this.periode = periode;
        this.utfall = utfallType;
        this.utbetalingsgrad = utbetalingsgrad;
    }

    public Periode getPeriode() {
        return periode;
    }

    public UtfallType getUtfall() {
        return utfall;
    }

    public BigDecimal getUtbetalingsgrad() {
        return utbetalingsgrad;
    }
}
