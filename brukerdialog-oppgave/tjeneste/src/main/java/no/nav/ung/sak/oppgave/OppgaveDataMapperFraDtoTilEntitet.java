package no.nav.ung.sak.oppgave;

import jakarta.enterprise.inject.Instance;
import no.nav.ung.sak.kontrakt.oppgaver.OppgaveType;
import no.nav.ung.sak.kontrakt.oppgaver.OppgavetypeDataDto;
import no.nav.ung.sak.oppgave.typer.OppgaveDataEntitet;

/**
 * Felles interface for alle klasser som mapper en {@link OppgavetypeDataDto}-subtype
 * til sin tilhørende JPA-entitet.
 * <p>
 * Implementasjoner skal annoteres med {@link OppgaveTypeRef} for CDI-oppslag.
 */
public interface OppgaveDataMapperFraDtoTilEntitet {

    static OppgaveDataMapperFraDtoTilEntitet finnTjeneste(Instance<OppgaveDataMapperFraDtoTilEntitet> utledere, OppgaveType oppgaveType) {
        return OppgaveTypeRef.Lookup.find(utledere, oppgaveType)
            .orElseThrow(() -> new IllegalArgumentException("Finner tjeneste for oppgavetype: " + oppgaveType));
    }

    /**
     * Mapper og persisterer oppgavedata til riktig JPA-entitet.
     *
     * @param data oppgavetypedata som skal mappes og persisteres
     * @return den persisterte entiteten
     */
    OppgaveDataEntitet map(OppgavetypeDataDto data);
}
