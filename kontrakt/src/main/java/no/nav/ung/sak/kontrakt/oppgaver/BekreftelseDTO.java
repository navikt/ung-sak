package no.nav.ung.sak.kontrakt.oppgaver;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.nav.ung.sak.kontrakt.oppgaver.typer.inntektsrapportering.RapportertInntektDto;

/**
 * Interface for bekreftelse-data.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = SvarPåVarselDTO.class, name = "VARSEL_SVAR"),
    @JsonSubTypes.Type(value = RapportertInntektDto.class, name = "RAPPORTERT_INNTEKT")
})
public class BekreftelseDTO { }

