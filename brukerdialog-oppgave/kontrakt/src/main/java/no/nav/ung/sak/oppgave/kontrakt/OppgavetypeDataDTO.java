package no.nav.ung.sak.oppgave.kontrakt;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Interface for oppgavetype-spesifikk data.
 * Alle oppgavetyper må implementere dette interfacet.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = no.nav.ung.sak.oppgave.kontrakt.typer.endretstartdato.EndretStartdatoDataDTO.class, name = "ENDRET_STARTDATO"),
    @JsonSubTypes.Type(value = no.nav.ung.sak.oppgave.kontrakt.typer.endretsluttdato.EndretSluttdatoDataDTO.class, name = "ENDRET_SLUTTDATO"),
    @JsonSubTypes.Type(value = no.nav.ung.sak.oppgave.kontrakt.typer.fjernperiode.FjernetPeriodeDataDTO.class, name = "FJERNET_PERIODE"),
    @JsonSubTypes.Type(value = no.nav.ung.sak.oppgave.kontrakt.typer.endretperiode.EndretPeriodeDataDTO.class, name = "ENDRET_PERIODE"),
    @JsonSubTypes.Type(value = no.nav.ung.sak.oppgave.kontrakt.typer.kontrollerregisterinntekt.KontrollerRegisterinntektOppgavetypeDataDTO.class, name = "KONTROLLER_REGISTERINNTEKT"),
    @JsonSubTypes.Type(value = no.nav.ung.sak.oppgave.kontrakt.typer.inntektsrapportering.InntektsrapporteringOppgavetypeDataDTO.class, name = "INNTEKTSRAPPORTERING"),
    @JsonSubTypes.Type(value = no.nav.ung.sak.oppgave.kontrakt.typer.søkytelse.SøkYtelseOppgavetypeDataDTO.class, name = "SØK_YTELSE")
})
public interface OppgavetypeDataDTO {
}

