package no.nav.ung.sak.kontrakt.oppgaver;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.nav.ung.sak.kontrakt.oppgaver.typer.endretperiode.EndretPeriodeDataDto;
import no.nav.ung.sak.kontrakt.oppgaver.typer.endretsluttdato.EndretSluttdatoDataDto;
import no.nav.ung.sak.kontrakt.oppgaver.typer.endretstartdato.EndretStartdatoDataDto;
import no.nav.ung.sak.kontrakt.oppgaver.typer.fjernperiode.FjernetPeriodeDataDto;
import no.nav.ung.sak.kontrakt.oppgaver.typer.inntektsrapportering.InntektsrapporteringOppgavetypeDataDto;
import no.nav.ung.sak.kontrakt.oppgaver.typer.kontrollerregisterinntekt.KontrollerRegisterinntektOppgavetypeDataDto;
import no.nav.ung.sak.kontrakt.oppgaver.typer.søkytelse.SøkYtelseOppgavetypeDataDto;

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
    @JsonSubTypes.Type(value = EndretStartdatoDataDto.class, name = "ENDRET_STARTDATO"),
    @JsonSubTypes.Type(value = EndretSluttdatoDataDto.class, name = "ENDRET_SLUTTDATO"),
    @JsonSubTypes.Type(value = FjernetPeriodeDataDto.class, name = "FJERNET_PERIODE"),
    @JsonSubTypes.Type(value = EndretPeriodeDataDto.class, name = "ENDRET_PERIODE"),
    @JsonSubTypes.Type(value = KontrollerRegisterinntektOppgavetypeDataDto.class, name = "KONTROLLER_REGISTERINNTEKT"),
    @JsonSubTypes.Type(value = InntektsrapporteringOppgavetypeDataDto.class, name = "INNTEKTSRAPPORTERING"),
    @JsonSubTypes.Type(value = SøkYtelseOppgavetypeDataDto.class, name = "SØK_YTELSE")
})
public interface OppgavetypeDataDto {
}

