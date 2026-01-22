package no.nav.ung.sak.etterlysning;

import no.nav.ung.sak.felles.typer.JournalpostId;

public record UttalelseData(boolean harUttalelse, String uttalelse, JournalpostId svarJournalpostId) {
}
