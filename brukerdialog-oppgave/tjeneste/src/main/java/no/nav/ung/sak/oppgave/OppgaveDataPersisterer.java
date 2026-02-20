package no.nav.ung.sak.oppgave;

import no.nav.ung.sak.kontrakt.oppgaver.OppgavetypeDataDTO;

/**
 * Felles interface for alle klasser som mapper og persisterer en {@link OppgavetypeDataDTO}-subtype
 * til sin tilhørende JPA-entitet.
 * <p>
 * Implementasjoner skal annoteres med {@link OppgaveTypeRef} for CDI-oppslag.
 */
public interface OppgaveDataPersisterer {

    /**
     * Mapper og persisterer oppgavedata til riktig JPA-entitet knyttet til den gitte oppgaven.
     * Oppgaven må være persistert før denne metoden kalles.
     *
     * @param oppgave oppgaven dataen tilhører
     * @param data    oppgavetypedata som skal lagres
     */
    void persister(BrukerdialogOppgaveEntitet oppgave, OppgavetypeDataDTO data);
}

