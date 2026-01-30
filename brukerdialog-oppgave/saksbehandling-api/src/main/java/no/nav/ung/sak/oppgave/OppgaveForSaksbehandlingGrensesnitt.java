package no.nav.ung.sak.oppgave;

import no.nav.ung.deltakelseopplyser.kontrakt.deltaker.DeltakerDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.endretperiode.EndretPeriodeOppgaveDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.EndreStatusDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.inntektsrapportering.InntektsrapporteringOppgaveDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.registerinntekt.RegisterInntektOppgaveDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.startdato.EndretSluttdatoOppgaveDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.startdato.EndretStartdatoOppgaveDTO;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Interface for å opprette og administrere brukerdialog-oppgaver.
 * Implementeres av både UngOppgaveKlient (REST-klient til ekstern tjeneste)
 * og BrukerdialogOppgaveTjeneste (intern håndtering i samme applikasjon).
 */
public interface OppgaveForSaksbehandlingGrensesnitt {

    /**
     * Oppretter en oppgave for å kontrollere registerinntekt.
     *
     * @param oppgave DTO med informasjon om registerinntekten som skal kontrolleres
     */
    void opprettKontrollerRegisterInntektOppgave(RegisterInntektOppgaveDTO oppgave);

    /**
     * Oppretter en oppgave for inntektsrapportering.
     *
     * @param oppgave DTO med informasjon om perioden det skal rapporteres inntekt for
     */
    void opprettInntektrapporteringOppgave(InntektsrapporteringOppgaveDTO oppgave);

    /**
     * Oppretter en oppgave for endret startdato.
     *
     * @param oppgave DTO med informasjon om den endrede startdatoen
     */
    void opprettEndretStartdatoOppgave(EndretStartdatoOppgaveDTO oppgave);

    /**
     * Oppretter en oppgave for endret sluttdato.
     *
     * @param oppgave DTO med informasjon om den endrede sluttdatoen
     */
    void opprettEndretSluttdatoOppgave(EndretSluttdatoOppgaveDTO oppgave);

    /**
     * Oppretter en oppgave for endret periode.
     *
     * @param oppgave DTO med informasjon om den endrede perioden
     */
    void opprettEndretPeriodeOppgave(EndretPeriodeOppgaveDTO oppgave);

    /**
     * Avbryter en oppgave basert på ekstern referanse.
     *
     * @param eksternRef unik referanse til oppgaven som skal avbryttes
     */
    void avbrytOppgave(UUID eksternRef);

    /**
     * Markerer en oppgave som utløpt basert på ekstern referanse.
     *
     * @param eksternRef unik referanse til oppgaven som skal markeres som utløpt
     */
    void oppgaveUtløpt(UUID eksternRef);

    /**
     * Setter oppgaver av en gitt type og periode til utløpt.
     *
     * @param dto DTO med informasjon om oppgavetype og periode
     */
    void settOppgaveTilUtløpt(EndreStatusDTO dto);

    /**
     * Setter oppgaver av en gitt type og periode til avbrutt.
     *
     * @param dto DTO med informasjon om oppgavetype og periode
     */
    void settOppgaveTilAvbrutt(EndreStatusDTO dto);

    /**
     * Løser en søk-ytelse-oppgave.
     *
     * @param deltakerDTO informasjon om deltakeren
     */
    void løsSøkYtelseOppgave(DeltakerDTO deltakerDTO);


    /**
     * Endrer frist for en oppgave.
     *
     * @param personIdent Personident for den oppgaven gjelder
     * @param eksternReferanse Oppgavereferanse
     * @param frist            Ny frist for oppgaven
     */
    void endreFrist(String personIdent, UUID eksternReferanse, LocalDateTime frist);
}

