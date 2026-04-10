package no.nav.ung.sak.kontrakt.aktivitetspenger.beregning;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public record AktivitetspengerSatsPeriodeDto(
    @NotNull LocalDate fom,
    @NotNull LocalDate tom,
    @NotNull BigDecimal dagsats,
    @NotNull BigDecimal grunnbeløpFaktor,
    @NotNull BigDecimal grunnbeløp,
    @NotNull AktivitetspengerSatsType satsType,
    int antallBarn,
    @NotNull int dagsatsBarnetillegg,
    @NotNull int antallDager
) {
}

