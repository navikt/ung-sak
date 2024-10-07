package no.nav.k9.sak.kontrakt.ungdomsytelse.uttak;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.k9.kodeverk.ungdomsytelse.uttak.UngdomsytelseUttakAvslagsårsak;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public record UngdomsytelseUttakPeriodeDto(
    @JsonProperty(value = "fom", required = true) LocalDate fom,
    @JsonProperty(value = "tom", required = true) LocalDate tom,
    @JsonProperty(value = "utbetalingsgrad", required = true) BigDecimal utbetalingsgrad,
    @JsonProperty(value = "avslagsårsak", required = false) UngdomsytelseUttakAvslagsårsak avslagsårsak
) {
}
