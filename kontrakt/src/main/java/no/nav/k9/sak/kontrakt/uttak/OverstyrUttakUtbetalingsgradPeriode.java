package no.nav.k9.sak.kontrakt.uttak;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.k9.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class OverstyrUttakUtbetalingsgradPeriode {


    @Valid
    @NotNull
    @JsonProperty("periode")
    private Periode periode;

    @Valid
    @NotNull
    @JsonProperty("uttaksgradPerioder")
    private UttakUtbetalingsgrad uttaksgrad;

    public OverstyrUttakUtbetalingsgradPeriode(Periode periode, UttakUtbetalingsgrad uttaksgrad) {
        this.periode = periode;
        this.uttaksgrad = uttaksgrad;
    }

    public Periode getPeriode() {
        return periode;
    }

    public UttakUtbetalingsgrad getUttaksgrad() {
        return uttaksgrad;
    }
}
