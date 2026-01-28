package no.nav.ung.sak.oppgave.brukerdialog;

import no.nav.ung.sak.felles.typer.AktørId;
import no.nav.ung.sak.oppgave.kontrakt.BrukerdialogOppgaveDto;

import java.util.List;
import java.util.UUID;

/**
 * Interface som definerer handlinger på oppgaver som er tilgjengelig for bruker.
 */
public interface BrukerdialogOppgaveTjeneste {

    /**
     * Henter alle oppgaver for en gitt aktør.
     *
     * @param aktørId aktørId for bruker
     * @return liste med oppgaver
     */
    List<BrukerdialogOppgaveDto> hentAlleOppgaverForAktør(AktørId aktørId);

    /**
     * Henter en spesifikk oppgave basert på oppgavereferanse og aktørId.
     *
     * @param oppgavereferanse unik referanse til oppgaven
     * @param aktørId aktørId for bruker
     * @return oppgaven som DTO
     * @throws IllegalArgumentException hvis oppgaven ikke finnes
     */
    BrukerdialogOppgaveDto hentOppgaveForOppgavereferanse(UUID oppgavereferanse, AktørId aktørId);

    /**
     * Lukker en oppgave basert på oppgavereferanse.
     *
     * @param oppgavereferanse unik referanse til oppgaven
     * @param aktørId aktørId for bruker
     * @return den lukkede oppgaven som DTO
     * @throws IllegalArgumentException hvis oppgaven ikke finnes
     */
    BrukerdialogOppgaveDto lukkOppgave(UUID oppgavereferanse, AktørId aktørId);

    /**
     * Åpner en oppgave basert på oppgavereferanse.
     *
     * @param oppgavereferanse unik referanse til oppgaven
     * @param aktørId aktørId for bruker
     * @return den åpnede oppgaven som DTO
     * @throws IllegalArgumentException hvis oppgaven ikke finnes
     */
    BrukerdialogOppgaveDto åpneOppgave(UUID oppgavereferanse, AktørId aktørId);

    /**
     * Løser en oppgave basert på oppgavereferanse.
     *
     * @param oppgavereferanse unik referanse til oppgaven
     * @param aktørId aktørId for bruker
     * @return den løste oppgaven som DTO
     * @throws IllegalArgumentException hvis oppgaven ikke finnes
     */
    BrukerdialogOppgaveDto løsOppgave(UUID oppgavereferanse, AktørId aktørId);
}
