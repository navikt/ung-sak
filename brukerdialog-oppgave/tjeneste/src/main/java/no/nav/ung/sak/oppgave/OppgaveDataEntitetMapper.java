package no.nav.ung.sak.oppgave;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.ung.sak.kontrakt.oppgaver.OppgavetypeDataDTO;

/**
 * Dispatcher som mapper og persisterer {@link OppgavetypeDataDTO} til riktig JPA-entitet.
 * Slår opp riktig {@link OppgaveDataPersisterer}-implementasjon via {@link OppgaveTypeRef} og CDI.
 */
@ApplicationScoped
public class OppgaveDataEntitetMapper {

    private Instance<OppgaveDataPersisterer> persistere;

    protected OppgaveDataEntitetMapper() {
        // CDI proxy
    }

    @Inject
    public OppgaveDataEntitetMapper(@Any Instance<OppgaveDataPersisterer> persistere) {
        this.persistere = persistere;
    }

    /**
     * Mapper og persisterer {@link OppgavetypeDataDTO} til riktig JPA-entitet knyttet til den gitte oppgaven.
     * Oppgaven må være persistert før denne metoden kalles.
     *
     * @param oppgave oppgaven dataen tilhører
     * @param data    oppgavetypedata som skal lagres
     */
    public void persister(BrukerdialogOppgaveEntitet oppgave, OppgavetypeDataDTO data) {
        var persisterer = OppgaveTypeRef.Lookup.find(persistere, oppgave.getOppgaveType())
            .orElseThrow(() -> new IllegalArgumentException(
                "Finner ingen OppgaveDataPersisterer for oppgavetype: " + oppgave.getOppgaveType()));
        persisterer.persister(oppgave, data);
    }
}
