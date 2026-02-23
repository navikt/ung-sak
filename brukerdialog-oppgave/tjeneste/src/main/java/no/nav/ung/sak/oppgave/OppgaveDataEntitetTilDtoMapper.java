package no.nav.ung.sak.oppgave;

import no.nav.ung.sak.kontrakt.oppgaver.OppgavetypeDataDto;
import no.nav.ung.sak.oppgave.typer.OppgaveDataEntitet;

/**
 * Mapper en {@link OppgaveDataEntitet}-subklasse til tilsvarende {@link OppgavetypeDataDto}.
 * Implementasjoner annoteres med {@link OppgaveTypeRef} for CDI-oppslag.
 */
public interface OppgaveDataEntitetTilDtoMapper {

    OppgavetypeDataDto tilDto(OppgaveDataEntitet entitet);
}

