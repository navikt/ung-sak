package no.nav.ung.sak.kontrakt.oppgaver.typer.søkytelse;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.sak.kontrakt.oppgaver.OppgaveType;
import no.nav.ung.sak.kontrakt.oppgaver.OppgavetypeDataDto;

import java.time.LocalDate;

/**
 * Data for søk ytelse oppgave.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    creatorVisibility = JsonAutoDetect.Visibility.NONE
)
public record SøkYtelseOppgavetypeDataDto(
    @JsonProperty(value = "fomDato", required = true)
    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    LocalDate fomDato
) implements OppgavetypeDataDto {
    @Override
    public OppgaveType oppgavetype() {
        return OppgaveType.SØK_YTELSE;
    }
}
