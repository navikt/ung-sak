package no.nav.k9.sak.kontrakt.uttak.søskensaker;

import java.math.BigDecimal;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.typer.Saksnummer;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public record PeriodeMedOverlapp(
        @Valid
        @JsonProperty(value = "periode")
        @NotNull
        Periode periode,
        @Valid
        @JsonProperty(value = "skalVurderes")
        @NotNull
        boolean skalVurderes,
        @Valid
        @JsonProperty(value = "fastsattUttaksgrad")
        @NotNull @DecimalMin("0")
        @DecimalMax("100")
        @Digits(integer = 3, fraction = 2)
        BigDecimal fastsattUttaksgrad,
        @Valid
        @JsonProperty(value = "saksnummer")
        @NotNull
        @Size(min = 1, max = 10)
        Set<Saksnummer> saksnummer
) {
}
