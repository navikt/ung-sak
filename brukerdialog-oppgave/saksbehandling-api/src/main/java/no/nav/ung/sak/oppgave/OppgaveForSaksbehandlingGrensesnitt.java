package no.nav.ung.sak.oppgave;

import no.nav.ung.sak.kontrakt.oppgaver.EndreOppgaveStatusDto;
import no.nav.ung.sak.kontrakt.oppgaver.typer.endretperiode.OpprettEndretPeriodeOppgaveDto;
import no.nav.ung.sak.kontrakt.oppgaver.typer.endretsluttdato.OpprettEndretSluttdatoOppgaveDto;
import no.nav.ung.sak.kontrakt.oppgaver.typer.endretstartdato.OpprettEndretStartdatoOppgaveDto;
import no.nav.ung.sak.kontrakt.oppgaver.typer.inntektsrapportering.OpprettInntektsrapporteringOppgaveDto;
import no.nav.ung.sak.kontrakt.oppgaver.typer.kontrollerregisterinntekt.OpprettKontrollerRegisterInntektOppgaveDto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Interface for å opprette og administrere brukerdialog-oppgaver.
 * Implementeres av både UngOppgaveKlient (REST-klient til ekstern tjeneste)
 * og BrukerdialogOppgaveTjeneste (intern håndtering i samme applikasjon).
 */
public interface OppgaveForSaksbehandlingGrensesnitt {

    default boolean isEnabled() {
        return true;
    }

    /**
     * Oppretter en oppgave for å kontrollere registerinntekt.
     */
    void opprettKontrollerRegisterInntektOppgave(OpprettKontrollerRegisterInntektOppgaveDto oppgave);

    /**
     * Oppretter en oppgave for inntektsrapportering.
     */
    void opprettInntektrapporteringOppgave(OpprettInntektsrapporteringOppgaveDto oppgave);

    /**
     * Oppretter en oppgave for endret startdato.
     */
    void opprettEndretStartdatoOppgave(OpprettEndretStartdatoOppgaveDto oppgave);

    /**
     * Oppretter en oppgave for endret sluttdato.
     */
    void opprettEndretSluttdatoOppgave(OpprettEndretSluttdatoOppgaveDto oppgave);

    /**
     * Oppretter en oppgave for endret periode.
     */
    void opprettEndretPeriodeOppgave(OpprettEndretPeriodeOppgaveDto oppgave);

    /**
     * Avbryter en oppgave basert på ekstern referanse.
     */
    void avbrytOppgave(UUID eksternRef);

    /**
     * Markerer en oppgave som utløpt basert på ekstern referanse.
     */
    void oppgaveUtløpt(UUID eksternRef);

    /**
     * Setter oppgaver av en gitt type og periode til utløpt.
     */
    void settOppgaveTilUtløpt(EndreOppgaveStatusDto dto);

    /**
     * Setter oppgaver av en gitt type og periode til avbrutt.
     */
    void settOppgaveTilAvbrutt(EndreOppgaveStatusDto dto);

    /**
     * Løser en søk-ytelse-oppgave.
     *
     * @param deltakerIdent personidentifikator for deltakeren
     */
    void løsSøkYtelseOppgave(String deltakerIdent);

    /**
     * Endrer frist for en oppgave.
     *
     * @param personIdent      personident for den oppgaven gjelder
     * @param eksternReferanse oppgavereferanse
     * @param frist            ny frist for oppgaven
     */
    void endreFrist(String personIdent, UUID eksternReferanse, LocalDateTime frist);
}
