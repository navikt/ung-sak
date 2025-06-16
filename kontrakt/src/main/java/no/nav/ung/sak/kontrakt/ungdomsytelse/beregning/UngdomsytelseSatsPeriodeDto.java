package no.nav.ung.sak.kontrakt.ungdomsytelse.beregning;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;
import no.nav.ung.kodeverk.ungdomsytelse.sats.UngdomsytelseSatsType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public record UngdomsytelseSatsPeriodeDto(
    @NotNull LocalDate fom,
    @NotNull LocalDate tom,
    @NotNull BigDecimal dagsats,
    @NotNull BigDecimal grunnbeløpFaktor,
    @NotNull BigDecimal grunnbeløp,
    @NotNull UngdomsytelseSatsType satsType,
    int antallBarn,
    @NotNull int dagsatsBarnetillegg,
    @NotNull int antallDager
) {
}
