package no.nav.ung.sak.kontrakt.oppgaver.typer.endretperiode;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.sak.kontrakt.oppgaver.OppgavetypeDataDto;

import java.util.Set;

/**
 * Data for oppgave om endret periode.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    creatorVisibility = JsonAutoDetect.Visibility.NONE
)
public record EndretPeriodeDataDto(
    @JsonProperty(value = "nyPeriode")
    PeriodeDTO nyPeriode,

    @JsonProperty(value = "forrigePeriode")
    PeriodeDTO forrigePeriode,

    @JsonProperty(value = "endringer", required = true)
    @NotNull
    Set<PeriodeEndringType> endringer
) implements OppgavetypeDataDto {
}

